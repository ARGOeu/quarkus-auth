package org.grnet.endpoint.scanner.runtime.endpoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@Schema(name="RevokeRoleRequest", description="A request to revoke a role from a specific user.")
public class RevokeRoleRequest {

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
            description = "The user's member id",
            example = "joe_doe"
    )
    @NotEmpty(message = "member_id may not be empty.")
    @JsonProperty("member_id")
    public String memberId;
}
