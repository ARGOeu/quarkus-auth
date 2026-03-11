package org.grnet.endpoint.scanner.runtime;

import java.util.Objects;

public class EndpointMetadata {

    private final String resource;
    private final String action;
    private final String path;
    private final String description;

    public EndpointMetadata(String resource, String action, String path, String description) {
        this.resource = resource;
        this.action = action;
        this.path = path;
        this.description = description;
    }

    public String getResource() {
        return resource;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndpointMetadata that)) return false;
        return Objects.equals(resource, that.resource)
                && Objects.equals(action, that.action)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, action, path);
    }
}
