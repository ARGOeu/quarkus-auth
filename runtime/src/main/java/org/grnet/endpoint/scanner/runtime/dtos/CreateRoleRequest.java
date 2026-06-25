package org.grnet.endpoint.scanner.runtime.dtos;

import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;

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
    @Schema(
            type = SchemaType.OBJECT,
            implementation = Map.class,
            description = "Optional group attributes.",
            example = "{\"description\":[\"Role with administrative rights\"],\"preferred_name\":[\"Admin\"]}"
    )
    public Map<String, List<String>> attributes;
}
