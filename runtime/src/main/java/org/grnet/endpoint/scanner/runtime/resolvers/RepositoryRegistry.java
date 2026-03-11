package org.grnet.endpoint.scanner.runtime.resolvers;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.RepositoryRegistrar;
import org.grnet.endpoint.scanner.runtime.entities.Repository;
import org.grnet.endpoint.scanner.runtime.entities.ResourceRepository;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.quarkus.arc.ComponentsProvider.LOG;


@ApplicationScoped
public class RepositoryRegistry {


    // Inject all beans in the container
    @Inject
    @Any
    Instance<Object> beans;

    private final Map<String, Object> repoMap = new HashMap<>();
    @Inject
    @Any
    Instance<RepositoryRegistrar> registrars;

    @PostConstruct
    void init() {
        registrars.forEach(r -> r.registerRepositories(this));
    }

    public void register(String key, Object repository) {
        if (repoMap.containsKey(key)) {
            throw new IllegalStateException("Duplicate repository for key: " + key);
        }
        repoMap.put(key, repository);
      }

    public Object getRepository(String resource) {
        Object repo = repoMap.get(resource);


        if (repo == null) {
            throw new IllegalStateException(
                    "No repository configured for resource: " + resource);
        }

        return repo;
    }

    public Object findEntity(String resource, Object id) {

        Object repo = getRepository(resource);


        try {
            Method method = repo.getClass().getMethod("findById", Object.class);
            return method.invoke(repo, id);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}