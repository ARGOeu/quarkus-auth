package org.grnet.endpoint.scanner.runtime.entitlements;

import java.util.List;

public interface EntitlementProvider {
    List<Entitlement> fetchEntitlements();
}
