package org.grnet.endpoint.scanner.runtime.entitlements;

import io.quarkus.oidc.TokenIntrospection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OIDCEntitlementService {

    @Inject
    TokenIntrospection tokenIntrospection;

    @Inject
    SecuredEndpointConfig config;

    /**
     * Extracts and parses entitlements from the OIDC token.
     */
    public List<Entitlement> fetchEntitlements() {

        var arr = tokenIntrospection.getJsonObject().getJsonArray("entitlements");

        if (arr == null) {
            return Collections.emptyList();
        }

        var raws = arr.stream()
                .map(v -> v.toString().replace("\"", ""))
                .filter(s -> s.startsWith(config.namespace()))           // filter by namespace
                .map(s -> s.replace(config.namespace() + ":", ""))
                .collect(Collectors.toList());

        return EntitlementUtils.parseEntitlements(raws);
    }

    public boolean isSuperAdmin(List<Entitlement> entitlements){

        return entitlements
                .stream()
                .anyMatch(entitlement -> entitlement.getGroup().equals(config.parentGroup()) && entitlement.getRole().equals(config.superAdminRole()));
    }
}
