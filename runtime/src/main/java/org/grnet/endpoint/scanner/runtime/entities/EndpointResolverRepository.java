package org.grnet.endpoint.scanner.runtime.entities;


import java.util.List;

public interface EndpointResolverRepository {

    List<EndpointResolver> list(String column, String id);
    void create(EndpointResolver entity);

    EndpointResolver findById(Long id);
    void update(EndpointResolver entity);
    void delete(Long id);
    List<EndpointResolver> findAllEndpointResolver();
}