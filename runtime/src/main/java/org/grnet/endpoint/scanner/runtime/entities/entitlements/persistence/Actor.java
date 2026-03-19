package org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence;

import java.time.LocalDateTime;
import java.util.Objects;

public class Actor {

    private String id;

    private String name;

    private String email;

    private LocalDateTime registeredOn;

    private String issuer;

    private String oidcId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getRegisteredOn() {
        return registeredOn;
    }

    public void setRegisteredOn(LocalDateTime registeredOn) {

        if(Objects.isNull(registeredOn)){
            registeredOn = LocalDateTime.now();
        }

        this.registeredOn = registeredOn;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getOidcId() {
        return oidcId;
    }

    public void setOidcId(String oidcId) {
        this.oidcId = oidcId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Actor actor = (Actor) o;
        return Objects.equals(oidcId, actor.oidcId) &&
                Objects.equals(issuer, actor.issuer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oidcId, issuer);
    }
}


