package org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroupInfoDto {

    @JsonProperty(value = "name")
    public String name;

    @JsonProperty("role")
    public String role;

    @JsonProperty("attributes")
    public Map<String, List<String>> attributes;
}
