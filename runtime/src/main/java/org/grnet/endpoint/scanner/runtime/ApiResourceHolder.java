package org.grnet.endpoint.scanner.runtime;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped

public class ApiResourceHolder {

     List<ApiResourceMetadata> data;
     List<ApiResourceMetadata> resources;

    public void setData(List<ApiResourceMetadata> data) {
        this.data = data;
    }

    public List<ApiResourceMetadata> getData() {
        return data;
    }

    public List<ApiResourceMetadata> getResources() {
        return resources;
    }

    public void setResources(List<ApiResourceMetadata> resources) {
        this.resources = resources;
    }
}

