package org.grnet.endpoint.scanner.deployment;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.panache.deployment.PanacheEntityClassBuildItem;
import io.quarkus.logging.Log;
import io.quarkus.undertow.deployment.ServletBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.grnet.endpoint.scanner.runtime.EndpointMetadataHolder;
import org.grnet.endpoint.scanner.runtime.EndpointRecorder;
import org.grnet.endpoint.scanner.runtime.entities.SecuredEndpoint;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointInterceptor;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointServlet;
import org.grnet.endpoint.scanner.runtime.database.SchemaInitializer;
import org.grnet.endpoint.scanner.runtime.endpoints.MyExtensionResource;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entitlements.OIDCEntitlementService;
import org.grnet.endpoint.scanner.runtime.services.SecuredEndpointService;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EndpointScannerProcessor {

    private static final String FEATURE = "endpoint-scanner";

    private static final Set<DotName> HTTP_METHODS = Set.of(
            DotName.createSimple("jakarta.ws.rs.GET"),
            DotName.createSimple("jakarta.ws.rs.POST"),
            DotName.createSimple("jakarta.ws.rs.PUT"),
            DotName.createSimple("jakarta.ws.rs.DELETE"),
            DotName.createSimple("jakarta.ws.rs.PATCH"),
            DotName.createSimple("jakarta.ws.rs.HEAD"),
            DotName.createSimple("jakarta.ws.rs.OPTIONS")
    );

    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    EndpointMetadataBuildItem scan(BeanArchiveIndexBuildItem indexBuildItem) {

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

            boolean isEndpoint = method.annotations().stream().anyMatch(a -> HTTP_METHODS.contains(a.name()));

            if (!isEndpoint) {
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

            var resource = annotation.value("resource").asString();
            var action = annotation.value("action").asString();
            var description = annotation.value("description").asString();

            addEndpoint(endpoints, new EndpointMetadata(resource, action, fullPath, description));
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
                    "Duplicate endpoint detected: resource=%s, action=%s, path=%s"
                            .formatted(endpoint.getResource(), endpoint.getAction(), endpoint.getPath()));
        }
        endpoints.add(endpoint);
    }

    @BuildStep
    ServletBuildItem createServlet() {

        return ServletBuildItem.builder("endpoint-scanner", SecuredEndpointServlet.class.getName())
                .addMapping("/secured-endpoints")
                .build();
    }

    @BuildStep
    void registerResource(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        additionalBeans.produce(new AdditionalBeanBuildItem(MyExtensionResource.class));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(MyExtensionResource.class.getName()));
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars, BuildProducer<AdditionalBeanBuildItem> beans) {

        registrars.produce(new InterceptorBindingRegistrarBuildItem(new AuthorizationAnnotationsRegistrar()));

        Class<?>[] interceptors = { SecuredEndpointInterceptor.class };
        beans.produce(new AdditionalBeanBuildItem(interceptors));
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerBeans() {
        return List.of(
                AdditionalBeanBuildItem.unremovableOf(OIDCEntitlementService.class),
                AdditionalBeanBuildItem.unremovableOf(EndpointMetadataHolder.class),
                AdditionalBeanBuildItem.unremovableOf(ResourceAuthorizationService.class),
                AdditionalBeanBuildItem.unremovableOf(SecuredEndpointService.class)
        );
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(SyntheticBeansRuntimeInitBuildItem.class)
    void syntheticBean(EndpointRecorder recorder, List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems, BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        var datasourceNames = getDataSourceNames(jdbcDataSourceBuildItems);

        var initializer = SyntheticBeanBuildItem
                .configure(SchemaInitializer.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .startup()
                .unremovable()
                .addInjectionPoint(ClassType.create(DotName.createSimple(AgroalDataSource.class)))
                .startup()
                .createWith(recorder.createSchemaInitializer())
                .checkActive(recorder.databaseCheckIsActive(datasourceNames));

        syntheticBeanBuildItemBuildProducer.produce(initializer.done());
    }

    @BuildStep
    @Consume(BeanContainerBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void startActions(EndpointRecorder recorder, BeanArchiveIndexBuildItem indexBuildItem) {

        var builder = scan(indexBuildItem);
        recorder.initSchema(builder.getEndpoints());
    }

    @BuildStep
    void registerEntities(BuildProducer<AdditionalJpaModelBuildItem> jpaModel, BuildProducer<PanacheEntityClassBuildItem> panacheEntities, CombinedIndexBuildItem index) {

        var entityClassInfo = index.getIndex()
                .getClassByName(DotName.createSimple(ResourceAuthorization.class.getName()));

        jpaModel.produce(new AdditionalJpaModelBuildItem(ResourceAuthorization.class.getName()));

        panacheEntities.produce(new PanacheEntityClassBuildItem(entityClassInfo));

        var securedEndpointClassInfo = index.getIndex()
                .getClassByName(DotName.createSimple(SecuredEndpoint.class.getName()));

        jpaModel.produce(new AdditionalJpaModelBuildItem(SecuredEndpoint.class.getName()));

        panacheEntities.produce(new PanacheEntityClassBuildItem(securedEndpointClassInfo));

    }

    private Set<String> getDataSourceNames(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems) {
        Set<String> result = new HashSet<>(jdbcDataSourceBuildItems.size());
        for (JdbcDataSourceBuildItem item : jdbcDataSourceBuildItems) {
            result.add(item.getName());
        }
        return result;
    }
}