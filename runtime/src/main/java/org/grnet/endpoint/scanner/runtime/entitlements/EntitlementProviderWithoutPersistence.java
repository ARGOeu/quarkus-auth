package org.grnet.endpoint.scanner.runtime.entitlements;

import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.OidcEntitlement;

import java.util.List;

public class EntitlementProviderWithoutPersistence implements EntitlementProvider {

    @Inject
    @OidcEntitlement
    EntitlementService entitlementService;

    @Inject
    SecuredEndpointConfig config;

    @Override
    public List<Entitlement> fetchEntitlements() {
        return entitlementService.fetchEntitlements();
    }

    @Override
    public boolean isSuperAdmin() {
        return fetchEntitlements()
                .stream()
                .anyMatch(entitlement -> entitlement.getGroup().equals(config.parentGroup()) && entitlement.getRole().equals(config.superAdminRole()));

    }
}
