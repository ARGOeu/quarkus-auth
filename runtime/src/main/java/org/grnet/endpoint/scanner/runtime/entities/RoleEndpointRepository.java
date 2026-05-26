package org.grnet.endpoint.scanner.runtime.entities;


import java.util.List;

public interface RoleEndpointRepository {

    List<RoleEndpoint> list(String column, String id);
    void create(RoleEndpoint entity);
    RoleEndpoint findById(Long id);
  //  void update(RoleEndpoint entity);
    void delete(Long id);
    void deleteByRoleIdAndEndpointId(String roleId,String securedEndpointId);

    void deleteByRoleIdAndEndpointIds(String roleId,List<String> securedEndpointId);
    void deleteByRoleId(String roleId);

    List<RoleEndpoint> findAll();
}