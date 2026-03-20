package org.grnet.endpoint.scanner.runtime.entitlements;

import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

import java.util.List;

public interface EntitlementProvider {
    List<Entitlement> fetchEntitlements();

    default boolean isSuperAdmin(SecuredEndpointConfig config){

        return fetchEntitlements()
                .stream()
                .anyMatch(entitlement -> entitlement.getGroup().equals(config.parentGroup()) && entitlement.getRole().equals(config.superAdminRole()));
    }
}
