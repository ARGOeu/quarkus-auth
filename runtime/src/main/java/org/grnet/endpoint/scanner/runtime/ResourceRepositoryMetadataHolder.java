package org.grnet.endpoint.scanner.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ResourceRepositoryMetadataHolder {

    private List<ResourceRepositoryMetadata> data;

    public void setData(List<ResourceRepositoryMetadata> data) {
        this.data = data;
    }

    public List<ResourceRepositoryMetadata> getData() {
        return data;
    }
}
