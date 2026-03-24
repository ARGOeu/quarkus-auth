package org.grnet.endpoint.scanner.runtime.entities;


import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class EndpointResolver  {
    private Long id;

    private String securedEndpointId;

    private String resource;

    private String originalField;

    private String mappedField;

    private LocalDateTime createdAt;


    public EndpointResolver(Long id, String securedEndpointId, String resource, String originalField, String mappedField, LocalDateTime createdAt) {
        this.id = id;
        this.securedEndpointId = securedEndpointId;
        this.resource = resource;
        this.originalField = originalField;
        this.mappedField = mappedField;
        this.createdAt = createdAt;
    }

    public EndpointResolver() {

    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getOriginalField() {
        return originalField;
    }

    public void setOriginalField(String originalField) {
        this.originalField = originalField;
    }

    public String getMappedField() {
        return mappedField;
    }

    public void setMappedField(String mappedField) {
        this.mappedField = mappedField;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
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

