package org.grnet.endpoint.scanner.runtime.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.util.Objects;

@Entity
@Table(name = "secured_endpoint")
public class SecuredEndpoint extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    @Column(name = "secured_endpoint_id", nullable = false, unique = true)
    private String securedEndpointId;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String description;

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

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var post = (SecuredEndpoint) o;
        return Objects.equals(securedEndpointId, post.securedEndpointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(securedEndpointId);
    }
}
