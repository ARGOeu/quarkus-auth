package org.grnet.endpoint.scanner.runtime.endpoints;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;

public class EndpointResolverRequest {

    @JsonProperty("resource")
    public String resource;

    @JsonProperty("original_field")
    public String original_field;


    @JsonProperty("mapped_field")
    public String mapped_field;
}
