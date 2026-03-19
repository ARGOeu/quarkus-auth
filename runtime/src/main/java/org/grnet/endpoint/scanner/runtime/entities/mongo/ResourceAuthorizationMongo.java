package org.grnet.endpoint.scanner.runtime.entities.mongo;

public class ResourceAuthorizationMongo  {

    private Long id;

    private String securedEndpointId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSecuredEndpointId() {
        return securedEndpointId;
    }

    public void setSecuredEndpointId(String securedEndpointId) {
        this.securedEndpointId = securedEndpointId;
    }
}
