package org.grnet.endpoint.scanner.runtime.endpoints;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AuthorizationRequest {

    @NotEmpty(message = "rules list must not be empty")
    public List<String> rules;
}
