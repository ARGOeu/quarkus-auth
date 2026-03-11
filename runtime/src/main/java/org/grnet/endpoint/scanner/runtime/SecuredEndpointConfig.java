package org.grnet.endpoint.scanner.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "api.auth.entitlements")   // ← prefix in application.properties
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SecuredEndpointConfig {

    /**
     * Namespace configuration.
     */
    String namespace();

    /**
     * Parent group configuration.
     */
    @WithName("parent-group")
    String parentGroup();

    /**
     * Super admin role configuration.
     */
    @WithName("super-admin-role")
    String superAdminRole();
}
