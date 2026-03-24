package org.grnet.endpoint.scanner.deployment;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.*;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.TokenIntrospection;
import jakarta.enterprise.context.ApplicationScoped;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.grnet.endpoint.scanner.runtime.EndpointMetadataHolder;
import org.grnet.endpoint.scanner.runtime.EndpointRecorder;
import org.grnet.endpoint.scanner.runtime.endpoints.PageLink;
import org.grnet.endpoint.scanner.runtime.endpoints.PageResource;
import org.grnet.endpoint.scanner.runtime.entities.PersistenceEntitlementRepository;
import org.grnet.endpoint.scanner.runtime.entities.jdbc.EndpointResolverJdbcRepository;
import org.grnet.endpoint.scanner.runtime.entities.jdbc.PersistenceEntitlementJDBCRepository;
import org.grnet.endpoint.scanner.runtime.entities.jdbc.ResourceAuthorizationJdbcRepository;
import org.grnet.endpoint.scanner.runtime.entities.mongo.EndpointResolverMongoRepository;
import org.grnet.endpoint.scanner.runtime.entities.mongo.ResourceAuthorizationMongoRepository;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Actor;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.ActorEntitlements;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Entitlement;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Setting;
import org.grnet.endpoint.scanner.runtime.entities.mongo.PersistenceEntitlementMongoRepository;
import org.grnet.endpoint.scanner.runtime.entities.mongo.ResourceAuthorizationMongo;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.ActorCodec;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.ActorEntitlementsCodec;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.EntitlementCodec;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.PersistenceEntitlementCodecProvider;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.ResourceAuthorizationCodec;
import org.grnet.endpoint.scanner.runtime.entities.mongo.codec.SettingCodec;
import org.grnet.endpoint.scanner.runtime.entities.pagination.Page;
import org.grnet.endpoint.scanner.runtime.entities.pagination.PageQuery;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProviderWithPersistence;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementProviderWithoutPersistence;
import org.grnet.endpoint.scanner.runtime.entitlements.EntitlementService;
import org.grnet.endpoint.scanner.runtime.entitlements.UserContextInterface;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.OidcEntitlement;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.PersistenceEntitlement;

import org.grnet.endpoint.scanner.runtime.entities.*;
import org.grnet.endpoint.scanner.runtime.resolvers.DynamicResolver;
import org.grnet.endpoint.scanner.runtime.resolvers.GroupIdResolver;
import org.grnet.endpoint.scanner.runtime.resolvers.RepositoryRegistry;
import org.grnet.endpoint.scanner.runtime.services.EndpointResolverService;
import org.grnet.endpoint.scanner.runtime.services.ResolverConfigService;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointInterceptor;
import org.grnet.endpoint.scanner.runtime.database.SchemaInitializer;
import org.grnet.endpoint.scanner.runtime.endpoints.SecuredEndpointResource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

class EndpointScannerProcessor {

    private static final Logger LOG = Logger.getLogger(EndpointScannerProcessor.class);

    private static final String FEATURE = "endpoint-scanner";

    private static final Map<DotName, String> HTTP_METHOD_NAMES = Map.of(
            DotName.createSimple("jakarta.ws.rs.GET"), "GET",
            DotName.createSimple("jakarta.ws.rs.POST"), "POST",
            DotName.createSimple("jakarta.ws.rs.PUT"), "PUT",
            DotName.createSimple("jakarta.ws.rs.DELETE"), "DELETE",
            DotName.createSimple("jakarta.ws.rs.PATCH"), "PATCH"
    );

    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    EndpointMetadataBuildItem scan(CombinedIndexBuildItem indexBuildItem) {

        // Quarkus provides the index automatically
        var index = indexBuildItem.getIndex();

        var endpoints = new ArrayList<EndpointMetadata>();

        var securedAnnotation = DotName.createSimple(
                "org.grnet.endpoint.scanner.runtime.SecuredEndpoint"
        );

        for (AnnotationInstance annotation : index.getAnnotations(securedAnnotation)) {

            if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue; // ignore class-level and interceptor usage
            }

            var method = annotation.target().asMethod();

            String httpMethod = method.annotations().stream()
                    .filter(a -> HTTP_METHOD_NAMES.containsKey(a.name()))
                    .map(a -> HTTP_METHOD_NAMES.get(a.name()))
                    .findFirst()
                    .orElse(null);

            if (httpMethod == null) {
                continue; // not a REST endpoint
            }

            // Read @Path from the method (e.g. "/items/{id}")
            String methodPath = "";
            var methodPathAnnotation = method.annotation(PATH);
            if (methodPathAnnotation != null) {
                methodPath = methodPathAnnotation.value().asString();
            }

            // Read @Path from the declaring class (e.g. "/api/v1")
            String classPath = "";
            var declaringClass = method.declaringClass();
            var classPathAnnotation = declaringClass.annotationsMap()
                    .getOrDefault(PATH, List.of())
                    .stream()
                    .filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                    .findFirst()
                    .orElse(null);

            if (classPathAnnotation != null) {
                classPath = classPathAnnotation.value().asString();
            }

            // Combine them, normalizing slashes
            var fullPath = normalizePath(classPath, methodPath);

            String description = "There is no description for this endpoint!";

            var operationAnnotation = method.annotation(DotName.createSimple("org.eclipse.microprofile.openapi.annotations.Operation"));

            if (operationAnnotation != null) {
                var descriptionValue = operationAnnotation.value("description");

                if (descriptionValue != null) {
                    description = descriptionValue.asString();
                }
            }

            addEndpoint(endpoints, new EndpointMetadata(generateSecuredEndpointId(httpMethod, fullPath), httpMethod, fullPath, description));
        }

        return new EndpointMetadataBuildItem(endpoints);
    }

    private String normalizePath(String classPath, String methodPath) {
        // Ensure class path starts with /
        if (!classPath.startsWith("/")) classPath = "/" + classPath;
        // Remove trailing slash from class path
        if (classPath.endsWith("/")) classPath = classPath.substring(0, classPath.length() - 1);
        // Ensure method path starts with /
        if (!methodPath.isEmpty() && !methodPath.startsWith("/")) methodPath = "/" + methodPath;

        return classPath + methodPath;
    }

    private void addEndpoint(List<EndpointMetadata> endpoints, EndpointMetadata endpoint) {

        if (endpoints.contains(endpoint)) {
            throw new IllegalArgumentException(
                    "Duplicate endpoint detected: action=%s, path=%s"
                            .formatted(endpoint.getAction(), endpoint.getPath()));
        }
        endpoints.add(endpoint);
    }

    private String generateSecuredEndpointId(String httpMethod, String path) {

        var raw = httpMethod + path;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate securedEndpointId hash", e);
        }
    }

    @BuildStep
    void registerResource(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        additionalBeans.produce(new AdditionalBeanBuildItem(SecuredEndpointResource.class));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(SecuredEndpointResource.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(ResourceAuthorizationMongo.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(Actor.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(Entitlement.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(ActorEntitlements.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(Setting.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(PageLink.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(PageResource.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(Page.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(PageQuery.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(SecuredEndpointResource.PageableSecuredEndpoints.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(EndpointMetadata.class.getName()));
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars, BuildProducer<AdditionalBeanBuildItem> beans) {

        registrars.produce(new InterceptorBindingRegistrarBuildItem(new AuthorizationAnnotationsRegistrar()));

        Class<?>[] interceptors = {SecuredEndpointInterceptor.class};
        beans.produce(new AdditionalBeanBuildItem(interceptors));
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerBeans() {

        return List.of(
                AdditionalBeanBuildItem.unremovableOf(EndpointMetadataHolder.class),
                AdditionalBeanBuildItem.unremovableOf(PersistenceEntitlementRepository.class),
                AdditionalBeanBuildItem.unremovableOf(ResourceAuthorizationService.class),
                AdditionalBeanBuildItem.unremovableOf(EndpointResolverService.class),
                AdditionalBeanBuildItem.unremovableOf(RepositoryRegistry.class),
                AdditionalBeanBuildItem.unremovableOf(DynamicResolver.class),
                AdditionalBeanBuildItem.unremovableOf(ResolverConfigService.class),
                AdditionalBeanBuildItem.unremovableOf(GroupIdResolver.class),
                AdditionalBeanBuildItem.unremovableOf(ResourceAuthorizationRepository.class),
                AdditionalBeanBuildItem.unremovableOf(EndpointResolverRepository.class),
                AdditionalBeanBuildItem.unremovableOf(EndpointResolver.class));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    void syntheticBean(EndpointRecorder recorder, BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                       List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {

        var names = getDataSourceNames(jdbcDataSourceBuildItems);

        var initializer = SyntheticBeanBuildItem
                .configure(SchemaInitializer.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .unremovable()
                .createWith(recorder.createSchemaInitializer())
                .checkActive(recorder.databaseCheckIsActive(names));

        var oidcEntitlementService = SyntheticBeanBuildItem
                .configure(EntitlementService.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .unremovable()
                .addQualifier()
                .annotation(DotName.createSimple(OidcEntitlement.class))
                .done()
                .addInjectionPoint(ClassType.create(DotName.createSimple(TokenIntrospection.class.getName())))
                .createWith(recorder.createOidcEntitlementService());

        if (jdbcDataSourceBuildItems.isEmpty()) {

            var persistenceEntitlementService = SyntheticBeanBuildItem
                    .configure(EntitlementService.class)
                    .scope(ApplicationScoped.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addQualifier()
                    .annotation(DotName.createSimple(PersistenceEntitlement.class))
                    .done()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(PersistenceEntitlementRepository.class.getName())))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(UserContextInterface.class.getName())))
                    .createWith(recorder.createPersistenceEntitlementService());

            syntheticBeanBuildItemBuildProducer.produce(persistenceEntitlementService.done());
        }

        syntheticBeanBuildItemBuildProducer.produce(initializer.done());
        syntheticBeanBuildItemBuildProducer.produce(oidcEntitlementService.done());
    }

    @BuildStep
    List<AdditionalBeanBuildItem> selectBeans(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {

        Class<?> resourceAuthorizationImplementation;
        Class<?> persistenceEntitlementImplementation;
        Class<?> entitlementProviderImplementation;
        Class<?> endpointResolverImplementation;

        if (!jdbcDataSourceBuildItems.isEmpty()) {
            resourceAuthorizationImplementation = ResourceAuthorizationJdbcRepository.class;
            persistenceEntitlementImplementation = PersistenceEntitlementJDBCRepository.class;
            entitlementProviderImplementation = EntitlementProviderWithoutPersistence.class;
            endpointResolverImplementation = EndpointResolverJdbcRepository.class;

        } else {

            resourceAuthorizationImplementation = ResourceAuthorizationMongoRepository.class;
            persistenceEntitlementImplementation = PersistenceEntitlementMongoRepository.class;
            entitlementProviderImplementation = EntitlementProviderWithPersistence.class;
            endpointResolverImplementation = EndpointResolverMongoRepository.class;
        }

        return List.of(AdditionalBeanBuildItem
                .builder()
                .addBeanClass(resourceAuthorizationImplementation)
                .setUnremovable()
                .build(), AdditionalBeanBuildItem
                .builder()
                .addBeanClass(endpointResolverImplementation)
                .setUnremovable()
                .build(), AdditionalBeanBuildItem
                .builder()
                .addBeanClass(persistenceEntitlementImplementation)
                .setUnremovable()
                .build(), AdditionalBeanBuildItem
                .builder()
                .addBeanClass(entitlementProviderImplementation)
                .setUnremovable()
                .build()
        );
    }

    @BuildStep(onlyIf = IsMongoPresent.class)
    void registerCodecProvider(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses) {
        LOG.info("Registering mongo related classes...");
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(ActorCodec.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(EntitlementCodec.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(ActorEntitlementsCodec.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(SettingCodec.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(ResourceAuthorizationCodec.class.getName()));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(PersistenceEntitlementCodecProvider.class.getName()));
    }

    public static class IsMongoPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("io.quarkus.mongodb.deployment.CodecProviderBuildItem");
                LOG.info("Mongo CodecProviderBuildItem found in class path...");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void startActions(EndpointRecorder recorder) {

        recorder.initSchema();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
        // or RUNTIME_INIT
    SecuredEndpointMetadataBuildItem processAndRecord(EndpointRecorder recorder, CombinedIndexBuildItem indexBuildItem) {

        var builder = scan(indexBuildItem);

        // Pass build-time data to the recorder → produces a RuntimeValue
        var metadata = recorder.storeSecuredEndpointMetadata(builder.getEndpoints());

        // Wire it into the CDI bean
        return new SecuredEndpointMetadataBuildItem(metadata);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureBeans(EndpointRecorder recorder, SecuredEndpointMetadataBuildItem configItem, BuildProducer<BeanContainerListenerBuildItem> listeners) {

        listeners.produce(new BeanContainerListenerBuildItem(recorder.configureBeanContainer(configItem.getEndpoints())));
    }

    @BuildStep
    void markResourceRepositoriesUnremovable(
            CombinedIndexBuildItem index,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        // Use the annotation class directly (works across modules)
        DotName annotation = DotName.createSimple(ResourceRepository.class.getName());

        index.getIndex().getAnnotations(annotation).forEach(a -> {
            var target = a.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                String className = target.asClass().name().toString();
                unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(className));
            }
        });
    }

    private Set<String> getDataSourceNames(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Set<String> result = new HashSet<>(jdbcDataSourceBuildItems.size());
        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            result.add(item.getName());
        }
        return result;
    }
}