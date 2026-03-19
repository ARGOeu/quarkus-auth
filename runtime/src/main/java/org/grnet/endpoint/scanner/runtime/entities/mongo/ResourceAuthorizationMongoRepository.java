package org.grnet.endpoint.scanner.runtime.entities.mongo;


import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.util.List;

public class ResourceAuthorizationMongoRepository implements ResourceAuthorizationRepository {
    @Override
    public List<?> findAll() {
        return List.of();
    }
}
