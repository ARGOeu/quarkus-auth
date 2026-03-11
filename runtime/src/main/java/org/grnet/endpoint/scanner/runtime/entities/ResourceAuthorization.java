package org.grnet.endpoint.scanner.runtime.entities;

import java.sql.Timestamp;

public class ResourceAuthorization {

    private Long id;

    private String rule;

    private String securedEndpointId;
    private Timestamp createdAt;

    public ResourceAuthorization(Long id, String securedEndpointId,String rule, Timestamp createdAt) {
        this.id = id;
        this.securedEndpointId=securedEndpointId;
        this.rule = rule;
        this.createdAt = createdAt;
    }

    public ResourceAuthorization() {

    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

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
