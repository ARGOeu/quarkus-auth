package org.grnet.endpoint.scanner.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EndpointMetadataHolder {

    private List<EndpointMetadata> data;

    public void setData(List<EndpointMetadata> data) {
        this.data = data;
    }

    public List<EndpointMetadata> getData() {
        return data;
    }
}
