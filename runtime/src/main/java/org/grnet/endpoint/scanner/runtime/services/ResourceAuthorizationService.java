package org.grnet.endpoint.scanner.runtime.services;

import io.quarkus.arc.InjectableInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationJdbcRepository;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationMongoRepository;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ResourceAuthorizationService {

    @Inject
    ResourceAuthorizationRepository repository;

    public List<?> findAll() {

        return repository.findAll();
    }
}
