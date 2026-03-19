package org.grnet.endpoint.scanner.runtime.entities;

import java.sql.Time;

public class ResourceAuthorization {

    private Long id;

    private String name;

    private Time createdAt;

    public ResourceAuthorization(Long id, String name, Time createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Time getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Time createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
