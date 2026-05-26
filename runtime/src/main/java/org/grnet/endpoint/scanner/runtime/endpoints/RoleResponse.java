package org.grnet.endpoint.scanner.runtime.endpoints;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="RoleResponse", description="Represents a role entity returned from the role management system.")
public class RoleResponse {

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The unique identifier of the role.",
            example = "759e745e-bc94-4488-a452-c08e1ebe6fe8"
    )
    public String id;

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The name of the role.",
            example = "admin"
    )
    public String name;

    public RoleResponse(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
