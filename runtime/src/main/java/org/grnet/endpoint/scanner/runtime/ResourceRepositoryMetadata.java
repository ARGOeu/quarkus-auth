package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ResourceRepositoryMetadata {

    private final String className;
    private final String value;

    public ResourceRepositoryMetadata(String className, String value) {
        this.className = className;
        this.value = value;
    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceRepositoryMetadata that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
