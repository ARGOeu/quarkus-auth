package org.grnet.endpoint.scanner.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.grnet.endpoint.scanner.runtime.ApiResourceMetadata;

import java.util.List;

public final class ApiResourcesBuildItem extends SimpleBuildItem {

    private final RuntimeValue<List<ApiResourceMetadata>> apiResources;

    public ApiResourcesBuildItem(RuntimeValue<List<ApiResourceMetadata>> apiResources) {
        this.apiResources = apiResources;
    }

    public RuntimeValue<List<ApiResourceMetadata>> getApiResources() {
        return apiResources;
    }
}
