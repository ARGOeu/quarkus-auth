package org.grnet.endpoint.scanner.runtime.internal;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.AuthGroupManagement;

@ApplicationScoped
public class AuthGroupInitializer {

    @Inject
    AuthGroupManagement authGroupManagement;

    void onStart(@Observes StartupEvent event) {
        //authGroupManagement.createParentGroup();
    }
}
