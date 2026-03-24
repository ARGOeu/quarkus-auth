package org.grnet.endpoint.scanner.runtime.endpoints;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.grnet.endpoint.scanner.runtime.SecuredEndpoint;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.services.EndpointResolverService;
import org.grnet.endpoint.scanner.runtime.services.ResourceAuthorizationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;

@Path("/secured-endpoints")
@Authenticated
@SecurityScheme(securitySchemeName = "Authentication",
        description = "JWT token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class SecuredEndpointResource {
    @Inject
    EndpointResolverService endpointResolverService;

    @Inject
    ResourceAuthorizationService resourceAuthorizationService;

    @Tag(name = "Secured Endpoints")
    @Operation(
            summary = "List all secured endpoints",
            description = "Returns a list of secured endpoints."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of all secured endpoints.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = PageableSecuredEndpoints.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SecuredEndpoint
    public Response getSecuredEndpoints(
            @Parameter(name = "page", in = QUERY, description = "Indicates the page number. Page number must be >= 1.")
            @DefaultValue("1")
            @Min(value = 1, message = "Page number must be >= 1.")
            @QueryParam("page")
            int page,
            @Parameter(name = "size", in = QUERY, description = "The page size.")
            @DefaultValue("10")
            @Min(value = 1, message = "Page size must be between 1 and 100.")
            @Max(value = 100, message = "Page size must be between 1 and 100.")
            @QueryParam("size")
            int size, @Context UriInfo uriInfo) {

        var securedEndpoints = resourceAuthorizationService.getSecuredEndpointsByPage(page - 1, size, uriInfo);

        return Response.ok().entity(securedEndpoints).build();
    }

    @Tag(name = "Secured Endpoints")
    @Operation(
            summary = "Create authorization rule for a secured endpoint.",
            description = "Create authorization rule."
    )
    @APIResponse(
            responseCode = "200",
            description = "Successfully created rule.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "409",
            description = "Rule already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @POST
    @Path("/{secured-endpoint-id}/authorizations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint
    public List<ResourceAuthorization> addAuthorizations(@Parameter(
            description = "The unique secured endpoint id.",
            required = true,
            example = "d9ae97f603809444ad911be3b30596462003d11bb06e928ac005bf4b1bb8c4a9",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("secured-endpoint-id") String id,
            @Valid @NotNull(message = "The request body is empty.") AuthorizationRequest request) {

        for (String regex : request.rules) {

            var re = new ResourceAuthorization();
            re.setSecuredEndpointId(id);
            re.setRule(regex);
            re.setCreatedAt(LocalDateTime.now());

            resourceAuthorizationService.authorize(re);
        }

        return resourceAuthorizationService.findAll();
    }

    @Tag(name = "Secured Endpoints")
    @Operation(
            summary = "Update authorization rules for a secured endpoint.",
            description = "Update authorization rules for a secured endpoint."
    )
    @APIResponse(
            responseCode = "200",
            description = "Successfully updated rules.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "409",
            description = "Rule already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @PUT
    @Path("/{secured-endpoint-id}/authorizations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint
    public List<ResourceAuthorization> updateAuthorizations(@Parameter(
            description = "The unique secured endpoint id.",
            required = true,
            example = "d9ae97f603809444ad911be3b30596462003d11bb06e928ac005bf4b1bb8c4a9",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("secured-endpoint-id") String securedEndpointId,
            @Valid @NotNull(message = "The request body is empty.") AuthorizationRequest request) {

        resourceAuthorizationService.updateAuthorizations(securedEndpointId, request.rules);

        return resourceAuthorizationService.findByEndpointSecuredEndpointId(securedEndpointId);
    }

    @Tag(name = "Secured Endpoints")
    @Operation(
            summary = "Create mapping between entity fields for endpoint  in order to resolve.",
            description = "Create mapping between entity fields for endpoint in order to resolve."
    )
    @APIResponse(
            responseCode = "200",
            description = "Successfully updated rules.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "401",
            description = "User has not been authenticated.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "403",
            description = "Not permitted.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "409",
            description = "Rule already exists.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error.",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    implementation = Object.class)))
    @POST
    @Path("/{secured-endpoint-id}/resolved")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @SecuredEndpoint
    public List<EndpointResolver> addResolvedField(@Parameter(
            description = "The unique secured endpoint id.",
            required = true,
            example = "d9ae97f603809444ad911be3b30596462003d11bb06e928ac005bf4b1bb8c4a9",
            schema = @Schema(type = SchemaType.STRING)) @PathParam("secured-endpoint-id") String id,
            @Valid @NotNull(message = "The request body is empty.") EndpointResolverRequest request) {

        var e = new EndpointResolver();
        e.setResource(request.resource);
        e.setMappedField(request.mapped_field);
        e.setOriginalField(request.original_field);
        e.setSecuredEndpointId(id);
        e.setCreatedAt(LocalDateTime.now());
        endpointResolverService.addResolvedField(e);

        return endpointResolverService.findAllEndpointResolverByEndpoint(id);
    }

    public static class PageableSecuredEndpoints extends PageResource<EndpointMetadata> {

        private List<EndpointMetadata> content;

        @Override
        public List<EndpointMetadata> getContent() {
            return content;
        }

        @Override
        public void setContent(List<EndpointMetadata> content) {
            this.content = content;
        }
    }
}





