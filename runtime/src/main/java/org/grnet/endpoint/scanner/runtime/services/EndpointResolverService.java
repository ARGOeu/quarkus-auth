package org.grnet.endpoint.scanner.runtime.services;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolverRepository;

import java.util.List;

@ApplicationScoped
public class EndpointResolverService {

    @Inject
    EndpointResolverRepository endpointResolverRepository;

    public List<EndpointResolver> findAllEndpointResolverByEndpoint(String id) {

        return endpointResolverRepository.list("secured_endpoint_id",id);
        //     return EndpointResolver.findBySecuredEndpointId(id);
    }

    public void addResolvedField(EndpointResolver endpointResolver) {
        endpointResolverRepository.create(endpointResolver);
    }

    @Transactional
    public void updateAuthorizations(String endpointId, List<String> requestedRules) {
    }

}
