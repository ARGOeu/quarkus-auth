package org.grnet.endpoint.scanner.runtime.entities.mongo;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@MongoEntity(collection = "resource_authorization")
public class ResourceAuthorizationMongo extends PanacheMongoEntityBase {

    @BsonId
    private Long id;

    @BsonProperty("secured_endpoint_id")
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
