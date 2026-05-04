package org.grnet.endpoint.scanner.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "api.auth")   // ← prefix in application.properties
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecuredEndpointConfig {

    /**
     * Unique user's id retrieving from OIDC token.
     */
    @WithName("oidc.user.unique.id")
    String oidcUserUniqueId();

    /**
     * Namespace configuration.
     */
    @WithName("entitlements.namespace")
    String namespace();

    /**
     * Parent group configuration.
     */
    @WithName("entitlements.parent-group")
    String parentGroup();

    /**
     * Super admin role configuration.
     */
    @WithName("entitlements.super-admin-role")
    String superAdminRole();

    /**
     * Keycloak group management client url.
     */
    @WithName("entitlements.keycloak-group-management-client-url")
    String keycloakGroupManagementClientUrl();

    /**
     * Keycloak group management client id.
     */
    @WithName("entitlements.keycloak-group-management-client-id")
    String keycloakGroupManagementClientId();

    /**
     * Keycloak group management client secret.
     */
    @WithName("entitlements.keycloak-group-management-client-secret")
    String keycloakGroupManagementClientSecret();
}
