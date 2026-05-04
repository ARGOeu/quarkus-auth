package org.grnet.endpoint.scanner.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ApiResourceHolder {

    private List<ApiResourceMetadata> data;

    public void setData(List<ApiResourceMetadata> data) {
        this.data = data;
    }

    public List<ApiResourceMetadata> getData() {
        return data;
    }
}
