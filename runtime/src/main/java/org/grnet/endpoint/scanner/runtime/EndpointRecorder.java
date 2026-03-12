package org.grnet.endpoint.scanner.runtime;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.grnet.endpoint.scanner.runtime.database.SchemaInitializer;
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

            var optional = names.stream().filter(DataSourceUtil::isDefault).findFirst();

            if(optional.isEmpty()){

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

        LOG.info("Secured Endpoints extension: Initializing schema...");

        var schemaInitializer = Arc.container().select(SchemaInitializer.class);

        if (schemaInitializer.isResolvable() && schemaInitializer.getHandle().getBean().isActive()) {

            var dbKind = ConfigProvider.getConfig().getValue("quarkus.datasource.db-kind", String.class);

            schemaInitializer.get().createTables(dbKind);
        } else {

            LOG.info("Secured Endpoints extension: Relational database schema initializer was deactivated...");
        }
    }

    public Function<SyntheticCreationalContext<SchemaInitializer>, SchemaInitializer> createSchemaInitializer() {
        return context -> {

            var dataSource = context.getInjectedReference(AgroalDataSource.class);
            return new SchemaInitializer(dataSource);
        };
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
}
