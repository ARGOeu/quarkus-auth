package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class EndpointMetadata {

    @JsonProperty("secured_endpoint_id")
    private final String securedEndpointId;
    private final String action;
    private final String path;
    private final String description;

    public EndpointMetadata(String securedEndpointId, String action, String path, String description) {
        this.securedEndpointId = securedEndpointId;
        this.action = action;
        this.path = path;
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getSecuredEndpointId() {
        return securedEndpointId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndpointMetadata that)) return false;
        return Objects.equals(action, that.action)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, path);
    }
}
