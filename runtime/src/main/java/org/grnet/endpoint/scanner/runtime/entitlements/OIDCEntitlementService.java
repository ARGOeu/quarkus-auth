package org.grnet.endpoint.scanner.runtime.entitlements;

import io.quarkus.oidc.TokenIntrospection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OIDCEntitlementService implements EntitlementService {

    private final TokenIntrospection tokenIntrospection;

    private final SecuredEndpointConfig config;

    public OIDCEntitlementService(TokenIntrospection tokenIntrospection, SecuredEndpointConfig config) {
        this.tokenIntrospection = tokenIntrospection;
        this.config = config;
    }

    /**
     * Extracts and parses entitlements from the OIDC token.
     */
    @Override
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
}
