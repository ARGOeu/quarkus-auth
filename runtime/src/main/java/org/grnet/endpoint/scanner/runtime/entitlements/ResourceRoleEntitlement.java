package org.grnet.endpoint.scanner.runtime.entitlements;

public record ResourceRoleEntitlement(
        String application,
        String role,
        String resource,
        String resourceId
) {}