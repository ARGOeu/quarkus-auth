package org.grnet.endpoint.scanner.runtime.endpoints;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;

import java.util.List;

@Path("/my-extension")
public class MyExtensionResource {

    @Inject
    ResourceAuthorizationService resourceAuthorizationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<?> hello() {
        return resourceAuthorizationService.findAll();
    }
}
