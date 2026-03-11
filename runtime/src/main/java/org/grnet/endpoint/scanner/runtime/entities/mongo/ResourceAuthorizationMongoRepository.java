package org.grnet.endpoint.scanner.runtime.entities.mongo;


import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.util.List;

public class ResourceAuthorizationMongoRepository implements ResourceAuthorizationRepository {
    @Override
    public List<ResourceAuthorization> findAll() {
        return List.of();
    }

    @Override
    public List list(String column, String id) {
        return List.of();
    }


    @Override
    public void create(ResourceAuthorization entity) {
    }
    }

