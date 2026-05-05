package org.grnet.endpoint.scanner.runtime.clients;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.KeycloakTokenClient;

import java.net.URI;


public class KeycloakClientCredentialsTokenProvider{

    private KeycloakTokenClient tokenClient;

    String clientId;

    String clientSecret;

    public KeycloakClientCredentialsTokenProvider(String url, String clientId, String clientSecret) {
        this.tokenClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(KeycloakTokenClient.class);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {

        return tokenClient
                .getToken("client_credentials", clientId, clientSecret)
                .access_token;
    }
}
