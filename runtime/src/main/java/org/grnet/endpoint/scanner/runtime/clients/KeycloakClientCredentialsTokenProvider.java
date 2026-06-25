package org.grnet.endpoint.scanner.runtime.clients;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.KeycloakTokenClient;

import java.net.URI;


public class KeycloakClientCredentialsTokenProvider{

    private volatile KeycloakTokenClient tokenClient;

    private final String url;
    private final String clientId;
    private final String clientSecret;

    public KeycloakClientCredentialsTokenProvider(String url, String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.url = url;
    }

    private KeycloakTokenClient getTokenClient() {
        if (tokenClient == null) {
            synchronized (this) {
                if (tokenClient == null) {
                    tokenClient = QuarkusRestClientBuilder.newBuilder()
                            .baseUri(URI.create(url))
                            .build(KeycloakTokenClient.class);
                }
            }
        }
        return tokenClient;
    }

    public String getAccessToken() {
        return getTokenClient()
                .getToken("client_credentials", clientId, clientSecret)
                .access_token;
    }
}
