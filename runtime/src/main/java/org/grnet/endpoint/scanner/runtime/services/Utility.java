package org.grnet.endpoint.scanner.runtime.services;

import io.quarkus.oidc.TokenIntrospection;
import jakarta.ws.rs.BadRequestException;
import org.grnet.endpoint.scanner.runtime.SecuredEndpointConfig;

public class Utility {

    private final TokenIntrospection tokenIntrospection;

    private final SecuredEndpointConfig config;

    public Utility(TokenIntrospection tokenIntrospection, SecuredEndpointConfig config) {
        this.tokenIntrospection = tokenIntrospection;
        this.config = config;
    }

    public String getUserUniqueIdentifier() {

        String id;

        try {
            id = tokenIntrospection.getJsonObject().getString(config.oidcUserUniqueId());
        } catch (Exception e) {

            String message = String.format("The User's unique identifier {%s} is missing from the access token.", config.oidcUserUniqueId());
            throw new BadRequestException(message);
        }

        return id;
    }

    public String getUsername() {
        try {
            return getUserUniqueIdentifier();
        } catch (Exception e) {
            throw new BadRequestException("Missing 'voperson_id' in access token.");
        }
    }

    public String getUserEmail() {
        return tokenIntrospection.getJsonObject().getString("email", null);
    }

    public String getUserName() {
        return tokenIntrospection.getJsonObject().getString("given_name", null);
    }

    public String getUserSurname() {
        return tokenIntrospection.getJsonObject().getString("family_name", null);
    }
}
