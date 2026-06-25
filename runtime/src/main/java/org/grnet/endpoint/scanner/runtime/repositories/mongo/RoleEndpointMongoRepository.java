package org.grnet.endpoint.scanner.runtime.repositories.mongo;

import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;

import java.util.List;

public class RoleEndpointMongoRepository implements RoleEndpointRepository {

//    @Inject
//    MongoClient mongoClient;
//
//    @ConfigProperty(name = "quarkus.mongodb.database")
//    String database;


    @Override
    public List<RoleEndpoint> list(String column, String id) {
        return List.of();
    }

    @Override
    public void create(RoleEndpoint entity) {

    }

    @Override
    public RoleEndpoint findById(Long id) {
        return null;
    }

    @Override
    public void update(RoleEndpoint entity) {

    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public void deleteByRoleIdAndEndpointId(String roleId, String securedEndpointId) {

    }

    @Override
    public void deleteByRoleIdAndEndpointIds(String roleId, List<String> securedEndpointId) {

    }

    @Override
    public void deleteByRoleId(String roleId) {

    }

    @Override
    public List<RoleEndpoint> findAll() {
        return List.of();
    }
}

