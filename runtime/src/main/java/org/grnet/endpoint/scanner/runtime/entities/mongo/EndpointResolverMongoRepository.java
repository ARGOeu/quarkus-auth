package org.grnet.endpoint.scanner.runtime.entities.mongo;


import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolverRepository;

import java.util.List;

public class EndpointResolverMongoRepository implements EndpointResolverRepository {
    @Override
    public List<EndpointResolver> findAll() {
        return List.of();
    }

    @Override
    public List list(String column, String id) {
        return List.of();
    }


    @Override
    public void create(EndpointResolver entity) {
    }
}

