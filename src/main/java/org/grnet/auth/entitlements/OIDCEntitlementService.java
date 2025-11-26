package org.grnet.auth.entitlements;

import io.quarkus.oidc.TokenIntrospection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class OIDCEntitlementService {

    @Inject
    TokenIntrospection tokenIntrospection;

    @ConfigProperty(name = "quarkus.auth.entitlements.namespace")
    String namespace;

    public List<Entitlement> fetchEntitlements() {
        var arr = tokenIntrospection.getJsonObject().getJsonArray("entitlements");
        if (arr == null) return Collections.emptyList();

        var raws = arr.stream()
                .map(v -> v.toString().replace("\"", ""))
                .filter(s -> s.startsWith(namespace))
                .map(s -> s.replace(namespace + ":", ""))
                .collect(Collectors.toList());

        return EntitlementUtils.parseEntitlements(raws);
    }
}
