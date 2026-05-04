package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class GroupRequest {


    @JsonProperty("name")
    public String name;

    @JsonProperty("attributes")
    public Map<String, List<String>> attributes;
}
