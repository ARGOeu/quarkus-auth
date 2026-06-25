package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import java.util.List;

public class Attributes {
    public List<String> uid;

    private List<String> localEntitlements;

    public List<String> getLocalEntitlements() {
        return localEntitlements;
    }
}
