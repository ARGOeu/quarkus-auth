package org.grnet.endpoint.scanner.runtime.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.smallrye.common.constraint.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.grnet.endpoint.scanner.runtime.Scope;

@Getter
@Setter
public class SecuredEndpointAssignment {

    @Schema(
            description = "Secured endpoint id",
            example = "123445-129393-13948"
    )
    @JsonProperty("secured_endpoint_id")
    @NotNull
    private String securedEndpointId;

    @Schema(
            description = "Scope of access",
            enumeration = {"ALL", "MINE"},
            example = "MINE"
    )

    private Scope scope;
}
