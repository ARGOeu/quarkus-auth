package org.grnet.endpoint.scanner.runtime.repositories;

import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;

import java.util.List;

public interface ResourceAuthorizationRepository {

    List<ResourceAuthorization> list(String column, String value);
    void  create(ResourceAuthorization entity);
    List<ResourceAuthorization> findAll();
    ResourceAuthorization findById(Long id);
    void update(Long id, String rule);
    void delete(Long id);
}