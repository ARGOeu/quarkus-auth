package org.grnet.endpoint.scanner.runtime.context;

import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;

import java.util.List;

public class RoleEndpointHolder {
    private static final ThreadLocal<List<RoleEndpoint>> ROLES = new ThreadLocal<>();

    public static void set(List<RoleEndpoint> roles) {
        ROLES.set(roles);
    }

    public static List<RoleEndpoint> get() {
        return ROLES.get();
    }

    public static void clear() {
        ROLES.remove();
    }
}