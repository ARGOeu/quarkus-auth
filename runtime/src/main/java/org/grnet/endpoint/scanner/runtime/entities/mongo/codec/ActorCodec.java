package org.grnet.endpoint.scanner.runtime.entities.mongo.codec;

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
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Actor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class ActorCodec implements CollectibleCodec<Actor> {

    private final Codec<Document> documentCodec;

    public ActorCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public Actor generateIdIfAbsentFromDocument(Actor actor) {
        if (!documentHasId(actor)) {
            actor.setId(new ObjectId().toString());
        }
        return actor;
    }

    @Override
    public boolean documentHasId(Actor actor) {
        return actor.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(Actor actor) {
        return new BsonString(actor.getId());
    }

    @Override
    public Actor decode(BsonReader bsonReader, DecoderContext decoderContext) {
        var document = documentCodec.decode(bsonReader, decoderContext);
        var actor = new Actor();
        actor.setId(document.getString("_id"));
        actor.setName(document.getString("name"));
        actor.setEmail(document.getString("email"));
        var date = (Date) document.get("registered_on");
        LocalDateTime localDateTime;
        if(Objects.isNull(date)){
            localDateTime = null;
        } else {

            localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        actor.setRegisteredOn(localDateTime);
        actor.setIssuer(document.getString("issuer"));
        actor.setOidcId(document.getString("oidc_id"));

        return actor;
    }

    @Override
    public void encode(BsonWriter bsonWriter, Actor actor, EncoderContext encoderContext) {

        var doc = new Document();
        doc.put("_id", actor.getId());
        doc.put("name", actor.getName());
        doc.put("email", actor.getEmail());
        doc.put("registered_on", actor.getRegisteredOn());
        doc.put("issuer", actor.getIssuer());
        doc.put("oidc_id", actor.getOidcId());

        documentCodec.encode(bsonWriter, doc, encoderContext);
    }

    @Override
    public Class<Actor> getEncoderClass() {
        return Actor.class;
    }
}
