package org.grnet.endpoint.scanner.runtime.entities;

import java.util.List;

public interface ResourceAuthorizationRepository {

    List<ResourceAuthorization> list(String column, String value);
    void  create(ResourceAuthorization entity);
    List<ResourceAuthorization> findAllResourceAuthorization();
    ResourceAuthorization findById(Long id);
    void update(ResourceAuthorization entity);
    void delete(Long id);

}