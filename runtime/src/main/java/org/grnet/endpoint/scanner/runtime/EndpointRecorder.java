package org.grnet.endpoint.scanner.runtime;

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import org.grnet.endpoint.scanner.runtime.database.SchemaInitializer;
import org.grnet.endpoint.scanner.runtime.entities.PersistenceEntitlementRepository;
import org.grnet.endpoint.scanner.runtime.entities.mongo.ResourceAuthorizationMongoRepository;
import org.grnet.endpoint.scanner.runtime.entitlements.OIDCEntitlementService;
import org.grnet.endpoint.scanner.runtime.entitlements.PersistenceEntitlementService;
import org.grnet.endpoint.scanner.runtime.entitlements.UserContextInterface;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

@Recorder
public class EndpointRecorder {

    private static final Logger LOG = Logger.getLogger(EndpointRecorder.class);

    public Supplier<ActiveResult> databaseCheckIsActive(Collection<String> names) {

        return () -> {

            if (names.isEmpty()) {
                return ActiveResult.inactive("There is no jdbc datasource.");
            }

            var optional = names.stream().filter(DataSourceUtil::isDefault).findFirst();

            if (optional.isEmpty()) {

                return ActiveResult.inactive("There is no default datasource.");
            } else {

                var dataSourceBean = AgroalDataSourceUtil.dataSourceInstance(optional.get()).getHandle().getBean();
                var dataSourceActive = dataSourceBean.checkActive();
                if (!dataSourceActive.value()) {
                    return ActiveResult.inactive(
                            String.format(Locale.ROOT, "Datasource '%s' was deactivated.", optional.get()), dataSourceActive);
                }

                return ActiveResult.active();
            }
        };
    }

    public void initSchema() {

      //  LOG.info("Secured Endpoints extension: Initializing schema...");

        var schemaInitializer = Arc.container().select(SchemaInitializer.class);

        if (schemaInitializer.getHandle().getBean().isActive()) {

            schemaInitializer.get().createTables();
        }
    }

    public Function<SyntheticCreationalContext<SchemaInitializer>, SchemaInitializer> createSchemaInitializer() {
        return context -> new SchemaInitializer();
    }

    public Function<SyntheticCreationalContext<ResourceAuthorizationMongoRepository>, ResourceAuthorizationMongoRepository> createMongoRepo() {
        return context -> new ResourceAuthorizationMongoRepository();
    }

    public RuntimeValue<List<EndpointMetadata>> storeSecuredEndpointMetadata(List<EndpointMetadata> data) {
        return new RuntimeValue<>(data);
    }

    public BeanContainerListener configureBeanContainer(RuntimeValue<List<EndpointMetadata>> metadata) {
        return beanContainer -> {
            var bean = beanContainer.beanInstance(EndpointMetadataHolder.class);
            bean.setData(metadata.getValue());
        };
    }

    public Function<SyntheticCreationalContext<PersistenceEntitlementService>, PersistenceEntitlementService> createPersistenceEntitlementService() {
        return context -> {
            // ServiceA is index 0, ServiceB is index 1
            var a = context.getInjectedReference(PersistenceEntitlementRepository.class);
            var b = context.getInjectedReference(UserContextInterface.class);

            return new PersistenceEntitlementService(a, b);
        };
    }

    public Function<SyntheticCreationalContext<OIDCEntitlementService>, OIDCEntitlementService> createOidcEntitlementService() {
        return context -> {
            // ServiceA is index 0, ServiceB is index 1
            var a = context.getInjectedReference(TokenIntrospection.class);

            var config = ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(SecuredEndpointConfig.class);

            return new OIDCEntitlementService(a, config);
        };
    }
}
