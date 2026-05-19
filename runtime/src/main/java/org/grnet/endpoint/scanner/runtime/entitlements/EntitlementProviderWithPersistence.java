package org.grnet.endpoint.scanner.runtime.entitlements;

import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.OidcEntitlement;
import org.grnet.endpoint.scanner.runtime.entitlements.qualifiers.PersistenceEntitlement;

import java.util.List;

public class EntitlementProviderWithPersistence implements EntitlementProvider{

    @Inject
    UserContextInterface userContextInterface;

    @Inject
    @OidcEntitlement
    EntitlementService oidcEntitlementService;

    @Inject
    @PersistenceEntitlement
    EntitlementService persistenceEntitlementService;

    @Inject
    SecuredEndpointConfig config;

    @Override
    public List<Entitlement> fetchEntitlements() {

        if(userContextInterface.entitlementManagement().equalsIgnoreCase("database")){

            return persistenceEntitlementService.fetchEntitlements();
        } else if (userContextInterface.entitlementManagement().equalsIgnoreCase("oidc")){

            return oidcEntitlementService.fetchEntitlements();
        } else {

            return persistenceEntitlementService.fetchEntitlements();
        }
    }

    @Override
    public boolean isSuperAdmin() {
        return fetchEntitlements()
                .stream()
                .anyMatch(entitlement -> entitlement.getGroup().equals(config.parentGroup()) && entitlement.getRole().equals(config.superAdminRole()));

    }
}
