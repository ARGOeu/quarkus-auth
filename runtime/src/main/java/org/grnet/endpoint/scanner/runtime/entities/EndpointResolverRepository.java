package org.grnet.endpoint.scanner.runtime.entities;


import java.util.List;

public interface EndpointResolverRepository {

    List<EndpointResolver> findAll();
    List<EndpointResolver> list(String column, String id);
    void create(EndpointResolver entity);
}