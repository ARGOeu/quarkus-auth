package org.grnet.endpoint.scanner.runtime.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Getter
@Setter
@Schema(name = "SecuredEndpointPerRoleRequest")
public class SecuredEndpointPerRoleRequest {

        @Schema(
                description = "List of secured endpoint assignments"
        )
        @JsonProperty("secured_endpoint_assignments")
        @NotEmpty
        private List<SecuredEndpointAssignment> assignments;
}