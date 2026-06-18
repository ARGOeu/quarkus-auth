package org.grnet;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.grnet.endpoint.scanner.runtime.ParamRef;
import org.grnet.endpoint.scanner.runtime.ParamType;
import org.grnet.endpoint.scanner.runtime.SecuredEndpoint;

@Path("/greeting")
@Authenticated
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SecuredEndpoint
    public String hello() {
        return "Hello from Quarkus Auth Example REST API!";
    }


    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @SecuredEndpoint(
            params = {

                    @ParamRef(
                            param = "id",
                            type = ParamType.PATH,
                            referTo = Greeting.class
                    )
            }
    )
    public String helloById(@PathParam("id") String id) {
        return "Hello from Greeting Resource with id "+id;
    }
}
