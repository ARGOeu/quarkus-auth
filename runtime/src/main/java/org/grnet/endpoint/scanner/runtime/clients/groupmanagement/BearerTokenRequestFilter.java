package org.grnet.endpoint.scanner.runtime.clients.groupmanagement;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.grnet.endpoint.scanner.runtime.clients.KeycloakClientCredentialsTokenProvider;

public class BearerTokenRequestFilter implements ClientRequestFilter {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;

    public BearerTokenRequestFilter(KeycloakClientCredentialsTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        var token = tokenProvider.getAccessToken();
        requestContext.getHeaders().putSingle("Authorization", "Bearer " + token);
    }
}
