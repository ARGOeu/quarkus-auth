package org.grnet.endpoint.scanner.runtime.context;

import jakarta.enterprise.context.RequestScoped;
import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequestScoped
public class RoleEndpointContext {

    private List<RoleEndpoint> roleEndpoints = new ArrayList<>();

    public void setRoleEndpoints(List<RoleEndpoint> roleEndpoints) {
        this.roleEndpoints = Collections.unmodifiableList(roleEndpoints);
    }

    public List<RoleEndpoint> getRoleEndpoints() {
        return roleEndpoints;
    }
}
