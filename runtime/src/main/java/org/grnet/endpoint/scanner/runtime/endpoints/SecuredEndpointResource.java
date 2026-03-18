package org.grnet.endpoint.scanner.runtime.endpoints;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.grnet.endpoint.scanner.runtime.dtos.PageResource;
import org.grnet.endpoint.scanner.runtime.services.SecuredEndpointService;

@Path("/secured-endpoints")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SecuredEndpointResource {

    @Inject
    SecuredEndpointService securedEndpointService;

    @GET
    public Response getSecuredEndpoints(
            @DefaultValue("1")
            @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,

            @DefaultValue("10")
            @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size,

            @Context UriInfo uriInfo) {

        var securedEndpoints = securedEndpointService.getByPage(page - 1, size);

        return Response.ok().entity(new PageResource<>(securedEndpoints, uriInfo)).build();
    }
}