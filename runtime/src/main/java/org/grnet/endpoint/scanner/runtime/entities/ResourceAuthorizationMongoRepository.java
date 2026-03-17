package org.grnet.endpoint.scanner.runtime.entities;

import org.grnet.endpoint.scanner.runtime.entities.mongo.ResourceAuthorizationMongo;

import java.util.List;

public class ResourceAuthorizationMongoRepository implements ResourceAuthorizationRepository {
    @Override
    public List<?> findAll() {
        return ResourceAuthorizationMongo.findAll().list();
    }
}
