package org.grnet.endpoint.scanner.runtime.services;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.repositories.EndpointResolverRepository;

import java.util.List;

@ApplicationScoped
public class EndpointResolverService {

    @Inject
    EndpointResolverRepository endpointResolverRepository;

    public List<EndpointResolver> findAllEndpointResolverByEndpoint(String id) {

        return endpointResolverRepository.list("secured_endpoint_id",id);
    }
    public List<EndpointResolver> findAllEndpointResolver() {
        return endpointResolverRepository.findAll();
    }
    public void delete(Long id) {
        endpointResolverRepository.delete(id);
    }
    public List<EndpointResolver> findByEndpointsecuredEndpointId(String securedEndpointId) {
        return endpointResolverRepository.list("secured_endpoint_id", securedEndpointId);
    }
    public EndpointResolver findById(Long id) {
        return endpointResolverRepository.findById(id);
    }

    public void update(EndpointResolver endpointResolver) {
        endpointResolverRepository.update(endpointResolver);
    }

    public void addResolvedField(EndpointResolver endpointResolver) {
        endpointResolverRepository.create(endpointResolver);
    }
}
