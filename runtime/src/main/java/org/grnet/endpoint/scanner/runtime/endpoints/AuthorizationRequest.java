package org.grnet.endpoint.scanner.runtime.endpoints;

import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "AuthorizationRequest represents a list of ResourceAuthorization objects of a secured endpoint to be created")
public class AuthorizationRequest {

    @NotEmpty(message = "rules list must not be empty")
    @Schema(
            type = SchemaType.ARRAY,
            implementation = List.class,
            description = "A list of rules to be created",
            example = "[\".*:tenants:{id}:topology:{topology-id}:.*\", \"group:status-pages:tenants:{id}:topology:{topology-id}:role=admin\"]"
    )
    public List<String> rules;
}
