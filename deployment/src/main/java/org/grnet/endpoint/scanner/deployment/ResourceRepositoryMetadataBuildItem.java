package org.grnet.endpoint.scanner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.grnet.endpoint.scanner.runtime.ResourceRepositoryMetadata;

import java.util.List;

public final class ResourceRepositoryMetadataBuildItem extends SimpleBuildItem {

    private final RuntimeValue<List<ResourceRepositoryMetadata>> repositories;

    public ResourceRepositoryMetadataBuildItem(RuntimeValue<List<ResourceRepositoryMetadata>> repositories) {
        this.repositories = repositories;
    }

    public RuntimeValue<List<ResourceRepositoryMetadata>> getRepositories() {
        return repositories;
    }
}
