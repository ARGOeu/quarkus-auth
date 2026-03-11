package org.grnet.endpoint.scanner.runtime;

import java.util.List;

public class EndpointMetadataHolder {

    private List<EndpointMetadata> data;


    public void setData(List<EndpointMetadata> data) {
        this.data = data;
    }

    public List<EndpointMetadata> getData() {
        return data;
    }
}
