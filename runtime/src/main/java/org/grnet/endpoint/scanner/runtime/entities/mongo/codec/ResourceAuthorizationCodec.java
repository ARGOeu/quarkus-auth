package org.grnet.endpoint.scanner.runtime.entities.mongo.codec;

import com.mongodb.MongoClientSettings;
import org.bson.BsonInt64;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class ResourceAuthorizationCodec implements CollectibleCodec<ResourceAuthorization> {

    private final Codec<Document> documentCodec;

    public ResourceAuthorizationCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public ResourceAuthorization generateIdIfAbsentFromDocument(ResourceAuthorization resourceAuthorization) {
        if (!documentHasId(resourceAuthorization)) {
            resourceAuthorization.setId(generateNewId());
        }
        return resourceAuthorization;
    }

    @Override
    public boolean documentHasId(ResourceAuthorization resourceAuthorization) {
        return resourceAuthorization.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(ResourceAuthorization resourceAuthorization) {
        return new BsonInt64(resourceAuthorization.getId());
    }

    @Override
    public ResourceAuthorization decode(BsonReader bsonReader, DecoderContext decoderContext) {

        var document = documentCodec.decode(bsonReader, decoderContext);
        var resourceAuthorization = new ResourceAuthorization();
        resourceAuthorization.setId(document.getLong("_id"));
        resourceAuthorization.setSecuredEndpointId(document.getString("secured_endpoint_id"));
        resourceAuthorization.setRule(document.getString("rule"));
        var date = (Date) document.get("created_at");
        LocalDateTime localDateTime;
        if(Objects.isNull(date)){
            localDateTime = null;
        } else {

            localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        resourceAuthorization.setCreatedAt(localDateTime);
        return resourceAuthorization;
    }

    @Override
    public void encode(BsonWriter bsonWriter, ResourceAuthorization resourceAuthorization, EncoderContext encoderContext) {

        var doc = new Document();
        doc.put("_id", resourceAuthorization.getId());
        doc.put("rule", resourceAuthorization.getRule());
        doc.put("created_at", resourceAuthorization.getCreatedAt());
        doc.put("secured_endpoint_id", resourceAuthorization.getSecuredEndpointId());
        documentCodec.encode(bsonWriter, doc, encoderContext);
    }

    @Override
    public Class<ResourceAuthorization> getEncoderClass() {
        return ResourceAuthorization.class;
    }

    private long generateNewId() {
        return UUID.randomUUID().getMostSignificantBits();
    }
}
