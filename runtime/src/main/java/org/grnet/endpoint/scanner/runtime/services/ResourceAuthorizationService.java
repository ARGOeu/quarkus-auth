package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.util.List;

@ApplicationScoped
public class ResourceAuthorizationService {

    @Inject
    ResourceAuthorizationRepository repository;

    public List<?> findAll() {

        return repository.findAll();
    }
}
