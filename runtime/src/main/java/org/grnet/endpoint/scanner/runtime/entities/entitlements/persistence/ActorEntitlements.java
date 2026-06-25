package org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class ActorEntitlements {

    private String id;

    private String actorId;

    private String entitlementId;

    private LocalDateTime assignedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(String entitlementId) {
        this.entitlementId = entitlementId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActorEntitlements actor = (ActorEntitlements) o;
        return Objects.equals(actorId, actor.actorId) &&
                Objects.equals(entitlementId, actor.entitlementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, entitlementId);
    }
}
