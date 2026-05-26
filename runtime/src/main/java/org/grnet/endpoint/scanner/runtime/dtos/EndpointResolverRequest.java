package org.grnet.endpoint.scanner.runtime.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;


@Schema(description = "EndpointResolverRequest represents an EndpointResolver object of a secured endpoint")

public class EndpointResolverRequest {

    @Schema(type = SchemaType.STRING,
            implementation = String.class,
            description = "Resource that defines the resolver field",
            example = "Tenants")

    @NotEmpty(message = "resource may not be empty.")
    @JsonProperty("resource")
    public String resource;

    @Schema(type = SchemaType.STRING,
            implementation = String.class,
            description = "The original field of the resource to be resolved",
            example = "id")
    @NotEmpty(message = "original field may not be empty.")
    @JsonProperty("original_field")
    public String original_field;

    @Schema(type = SchemaType.STRING,
            implementation = String.class,
            description = "The mapped field of the resource in which the original_field resolves",
            example = "name")
    @NotEmpty(message = "mapped field may not be empty.")
    @JsonProperty("mapped_field")
    public String mapped_field;
}
