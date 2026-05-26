package org.grnet.endpoint.scanner.runtime.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Schema(name="AssignRoleRequest", description="A request to assign role to a specific user.")
public class AssignRoleRequest {

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The resource name",
            example = "Tenant"
    )
    @JsonProperty("api_resource")
    public String apiResource;

    @Schema(
            type = SchemaType.OBJECT,
            implementation = Object.class,
            description = "The resource id",
            example = "5"
    )
    @JsonProperty("resource_id")
    public String resourceId;

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The role name",
            example = "admin"
    )
    @NotEmpty(message = "role may not be empty.")
    public String role;

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The user's username",
            example = "joe_doe"
    )
    @NotEmpty(message = "username may not be empty.")
    public String username;

    @Schema(
            type = SchemaType.OBJECT,
            additionalProperties = Object.class,
            description = "Extra request parameters."
    )
    public Map<String, Object> extras;
}
