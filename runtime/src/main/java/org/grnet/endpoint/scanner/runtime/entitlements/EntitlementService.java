package org.grnet.endpoint.scanner.runtime.entitlements;

import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

import java.util.List;

public interface EntitlementService {

    List<Entitlement> fetchEntitlements();
}
