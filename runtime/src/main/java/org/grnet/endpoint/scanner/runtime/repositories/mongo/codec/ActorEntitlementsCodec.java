package org.grnet.endpoint.scanner.runtime.repositories.mongo.codec;

import com.mongodb.MongoClientSettings;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.ActorEntitlements;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Entitlement;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class ActorEntitlementsCodec implements CollectibleCodec<ActorEntitlements> {

    private final Codec<Document> documentCodec;

    public ActorEntitlementsCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public ActorEntitlements generateIdIfAbsentFromDocument(ActorEntitlements actorEntitlements) {
        if (!documentHasId(actorEntitlements)) {
            actorEntitlements.setId(new ObjectId().toString());
        }
        return actorEntitlements;
    }

    @Override
    public boolean documentHasId(ActorEntitlements actorEntitlements) {
        return actorEntitlements.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(ActorEntitlements actorEntitlements) {
        return new BsonString(actorEntitlements.getId());
    }

    @Override
    public ActorEntitlements decode(BsonReader bsonReader, DecoderContext decoderContext) {
        var document = documentCodec.decode(bsonReader, decoderContext);
        var actorEntitlements = new ActorEntitlements();
        actorEntitlements.setId(document.getString("_id"));
        actorEntitlements.setEntitlementId(document.getString("entitlement_id"));
        actorEntitlements.setActorId(document.getString("actor_id"));
        var date = (Date) document.get("assigned_at");
        LocalDateTime localDateTime;
        if(Objects.isNull(date)){
            localDateTime = null;
        } else {

            localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        actorEntitlements.setAssignedAt(localDateTime);

        return actorEntitlements;
    }

    @Override
    public void encode(BsonWriter bsonWriter, ActorEntitlements actorEntitlements, EncoderContext encoderContext) {

        var doc = new Document();
        doc.put("_id", actorEntitlements.getId());
        doc.put("entitlement_id", actorEntitlements.getEntitlementId());
        doc.put("actor_id", actorEntitlements.getActorId());
        doc.put("assigned_at", actorEntitlements.getAssignedAt());

        documentCodec.encode(bsonWriter, doc, encoderContext);
    }

    @Override
    public Class<ActorEntitlements> getEncoderClass() {
        return ActorEntitlements.class;
    }
}
