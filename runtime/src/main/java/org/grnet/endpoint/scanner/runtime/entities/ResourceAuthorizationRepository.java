package org.grnet.endpoint.scanner.runtime.entities;

import java.util.List;

public interface ResourceAuthorizationRepository {

    List<ResourceAuthorization> findAll();
    List<ResourceAuthorization> list(String column, String value);
    void  create(ResourceAuthorization entity);
}