package org.grnet.endpoint.scanner.runtime.entitlements;

import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.OidcEntitlement;

import java.util.List;

public class EntitlementProviderWithoutPersistence implements EntitlementProvider{

    @Inject
    @OidcEntitlement
    EntitlementService entitlementService;

    @Override
    public List<Entitlement> fetchEntitlements() {
        return entitlementService.fetchEntitlements();
    }
}
