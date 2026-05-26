package org.grnet.endpoint.scanner.runtime.repositories.mongo.codec;

import org.bson.BsonInt64;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ResourceAuthorizationCodec implements CollectibleCodec<ResourceAuthorization> {

    public ResourceAuthorizationCodec() {
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
    public ResourceAuthorization decode(BsonReader reader, DecoderContext decoderContext) {
        var entity = new ResourceAuthorization();
        reader.readStartDocument();

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();

            switch (fieldName) {
                case "_id":
                    entity.setId(reader.readInt64());
                    break;
                case "secured_endpoint_id":
                    entity.setSecuredEndpointId(reader.readString());
                    break;
                case "rule":
                    entity.setRule(reader.readString());
                    break;
                case "created_at":
                    long millis = reader.readDateTime();
                    entity.setCreatedAt(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(millis),
                            ZoneId.systemDefault()
                    ));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.readEndDocument();
        return entity;
    }

    @Override
    public void encode(BsonWriter bsonWriter, ResourceAuthorization resourceAuthorization, EncoderContext encoderContext) {

        bsonWriter.writeStartDocument();
        bsonWriter.writeInt64("_id", resourceAuthorization.getId());
        bsonWriter.writeString("rule", resourceAuthorization.getRule());
        bsonWriter.writeString("secured_endpoint_id", resourceAuthorization.getSecuredEndpointId());
        bsonWriter.writeDateTime("created_at", resourceAuthorization.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        bsonWriter.writeEndDocument();
    }

    @Override
    public Class<ResourceAuthorization> getEncoderClass() {
        return ResourceAuthorization.class;
    }

    private long generateNewId() {
        return System.currentTimeMillis();
    }
}
