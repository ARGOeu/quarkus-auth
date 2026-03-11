package org.grnet.endpoint.scanner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;

import java.util.List;

public final class EndpointMetadataBuildItem extends SimpleBuildItem {

    private final List<EndpointMetadata> endpoints;

    public EndpointMetadataBuildItem(List<EndpointMetadata> endpoints) {
        this.endpoints = endpoints;
    }

    public List<EndpointMetadata> getEndpoints() {
        return endpoints;
    }
}
