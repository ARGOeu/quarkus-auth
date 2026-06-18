package org.grnet;

import org.grnet.endpoint.scanner.runtime.ApiResource;

public enum Greeting implements ApiResource {
    Greeting;

    @Override
    public String resourceName() {
        return "Greeting";
    }
}
