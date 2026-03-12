package org.grnet.endpoint.scanner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;

import java.util.List;

public final class SecuredEndpointMetadataBuildItem extends SimpleBuildItem {

    private final RuntimeValue<List<EndpointMetadata>> endpoints;

    public SecuredEndpointMetadataBuildItem(RuntimeValue<List<EndpointMetadata>> endpoints) {
        this.endpoints = endpoints;
    }

    public RuntimeValue<List<EndpointMetadata>> getEndpoints() {
        return endpoints;
    }
}
