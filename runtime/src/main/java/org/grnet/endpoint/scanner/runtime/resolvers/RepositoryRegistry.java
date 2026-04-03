package org.grnet.endpoint.scanner.runtime.resolvers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.ResourceRepositoryMetadata;
import org.grnet.endpoint.scanner.runtime.ResourceRepositoryMetadataHolder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;


@ApplicationScoped
public class RepositoryRegistry {

    @Inject
    ResourceRepositoryMetadataHolder resourceRepositoryMetadataHolder;

    public Object findEntity(String resource, Object id) {

        return findByResource(resource, resourceRepositoryMetadataHolder.getData())
                .map(r -> {
                    try {
                        Class<?> clazz = Thread.currentThread()
                                .getContextClassLoader()
                                .loadClass(r.getClassName());

                        Object repo = CDI.current()
                                .select(clazz, Any.Literal.INSTANCE)
                                .get();

                        Method method = clazz.getMethod("findById", Object.class);

                        return method.invoke(repo, id);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resource));
    }

    public Optional<ResourceRepositoryMetadata> findByResource(
            String resource,
            List<ResourceRepositoryMetadata> data) {

        return data.stream()
                .filter(item -> item.getValue().equalsIgnoreCase(resource))
                .findFirst();
    }
}