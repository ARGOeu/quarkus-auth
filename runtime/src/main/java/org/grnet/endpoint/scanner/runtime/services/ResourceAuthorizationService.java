package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.util.List;

@ApplicationScoped
public class ResourceAuthorizationService {


    @Inject
    ResourceAuthorizationRepository repository;

    public List<ResourceAuthorization> findAll() {
        return repository.findAll();
    }

    public List<?> findByEndpointId(String id) {
        return repository.list("id", id);
    }
//
//    public void authorize(ResourceAuthorization re) {
//
//        repository.create(re);
//    }

    public void authorize(ResourceAuthorization re) {

        repository.create(re);
    }
    public List<ResourceAuthorization> findByEndpointsecuredEndpointId(String securedEndpointId) {
        return repository.list("secured_endpoint_id", securedEndpointId);
    }

    @Transactional
    public void updateAuthorizations(String endpointId, List<String> requestedRules) {
    }


}
