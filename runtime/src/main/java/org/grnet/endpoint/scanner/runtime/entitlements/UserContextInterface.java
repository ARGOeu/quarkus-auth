package org.grnet.endpoint.scanner.runtime.entitlements;

public interface UserContextInterface {

    String getId();

    String getIssuer();

    String getNamespace();

    public String entitlementManagement();
}
