package org.grnet.templates;

import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/")
public class HtmlTemplate {

    @Inject
    Template client;

    @Inject
    Template index;

    @ConfigProperty(name = "api.html.oidc.client.url")
    String statusOidcClientUrl;

    @ConfigProperty(name = "api.html.swagger.documentation")
    String statusApiDocumentation;

    @ConfigProperty(name = "api.html.keycloak.url")
    String keycloakServerUrl;

    @ConfigProperty(name = "api.html.keycloak.realm")
    String keycloakServerRealm;

    @ConfigProperty(name = "api.html.keycloak.public.client.id")
    String keycloakServerClientId;

    @ConfigProperty(name = "api.html.keycloak.javascript.adapter")
    String keycloakServerJavascriptAdapter;

    @Tag(name = "Authentication")
    @Operation(
            hidden = true
    )
    @APIResponse(
            responseCode = "200",
            description = "Returns the index html statuspage."
    )

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String landingPage() {
        return index
                .data("status_oidc_client_url", statusOidcClientUrl,
                "status_api_documentation",statusApiDocumentation)
                .render();
    }

    @Tag(name = "Html Template")
    @Operation(
            hidden = true,
            summary = "Redirects a client to the AAI login statuspage.",
            description = "The user is presented with the AAI login statuspage, which is hosted by the authentication server. " +
                    "This statuspage contains options for selecting the user's identity provider (IdP) or organization. After selecting the identity provider, the user is redirected to the selected IdP's login statuspage to enter their credentials." +
                    " The user enters their username and password  on the IdP's login statuspage. The identity provider verifies the user's credentials and determines whether they are valid. If the authentication is successful, the IdP generates an access token.")
    @APIResponse(
            responseCode = "200",
            description = "Returns an html statuspage containing the obtained access token."
    )

    @GET
    @Path("oidc-client")
    @Produces(MediaType.TEXT_HTML)
    public String oidcClient() {
        return client
                .data("keycloak_server_url", keycloakServerUrl,
                        "keycloak_server_realm", keycloakServerRealm,
                        "keycloak_server_client_id", keycloakServerClientId,
                        "keycloak_server_javascript_adapter", keycloakServerJavascriptAdapter)
                .render();
    }
}
