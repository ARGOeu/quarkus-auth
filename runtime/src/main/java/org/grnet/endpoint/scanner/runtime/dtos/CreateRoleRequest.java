package org.grnet.endpoint.scanner.runtime.dtos;

import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="CreateRoleRequest", description="A request to create a new role.")
public class CreateRoleRequest {

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "The role name",
            example = "admin"
    )
    @NotEmpty(message = "name may not be empty.")
    public String name;
}
