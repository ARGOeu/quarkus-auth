package org.grnet.endpoint.scanner.runtime.endpoints;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SecuredEndpointRegistry {

    private final Map<String, Boolean> requiresScope = new HashMap<>();

    public void register(String endpointId, boolean requiresScope) {
        this.requiresScope.put(endpointId, requiresScope);
    }

    public boolean requiresScope(String endpointId) {
        return requiresScope.getOrDefault(endpointId, false);
    }
}
