package org.grnet.endpoint.scanner.runtime.endpoints;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public class EndpointResolverRequest {

    @NotEmpty(message = "resource may not be empty.")
    @JsonProperty("resource")
    public String resource;

    @NotEmpty(message = "original_field may not be empty.")
    @JsonProperty("original_field")
    public String original_field;

    @NotEmpty(message = "mapped_field may not be empty.")
    @JsonProperty("mapped_field")
    public String mapped_field;
}
