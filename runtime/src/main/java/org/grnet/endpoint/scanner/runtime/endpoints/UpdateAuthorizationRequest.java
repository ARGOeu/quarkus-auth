package org.grnet.endpoint.scanner.runtime.endpoints;

import com.mongodb.lang.NonNull;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "UpdateAuthorizationRequest represents an ResourceAuthorization object of a secured endpoint to be updated")

public class UpdateAuthorizationRequest {
    @Schema(type = SchemaType.STRING,
            implementation = String.class,
            description = "The rule to be updated",
            example = ".*:tenants:{id}:topology:{topology-id}:.*")
    @NotEmpty(message = "rule may not be empty.")
    public String rule; // new rule value
}