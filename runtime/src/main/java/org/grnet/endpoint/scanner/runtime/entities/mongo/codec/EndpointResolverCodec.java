package org.grnet.endpoint.scanner.runtime.entities.mongo.codec;

import org.bson.BsonInt64;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class EndpointResolverCodec implements CollectibleCodec<EndpointResolver> {

    public EndpointResolverCodec() {
    }

    @Override
    public EndpointResolver generateIdIfAbsentFromDocument(EndpointResolver endpointResolver) {
        if (!documentHasId(endpointResolver)) {
            endpointResolver.setId(generateNewId());
        }
        return endpointResolver;
    }

    @Override
    public boolean documentHasId(EndpointResolver endpointResolver) {
        return endpointResolver.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(EndpointResolver endpointResolver) {
        return new BsonInt64(endpointResolver.getId());
    }

    @Override
    public EndpointResolver decode(BsonReader reader, DecoderContext decoderContext) {

        var entity = new EndpointResolver();
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
                case "resource":
                    entity.setResource(reader.readString());
                    break;
                case "mapped_field":
                    entity.setMappedField(reader.readString());
                case "original_field":
                    entity.setOriginalField(reader.readString());
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
    public void encode(BsonWriter bsonWriter, EndpointResolver endpointResolver, EncoderContext encoderContext) {

        bsonWriter.writeStartDocument();
        bsonWriter.writeInt64("_id", endpointResolver.getId());
        bsonWriter.writeString("resource", endpointResolver.getResource());
        bsonWriter.writeString("secured_endpoint_id", endpointResolver.getSecuredEndpointId());
        bsonWriter.writeString("mapped_field", endpointResolver.getMappedField());
        bsonWriter.writeString("original_field", endpointResolver.getOriginalField());
        bsonWriter.writeDateTime("created_at", endpointResolver.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        bsonWriter.writeEndDocument();
    }

    @Override
    public Class<EndpointResolver> getEncoderClass() {
        return EndpointResolver.class;
    }

    private long generateNewId() {
        return System.currentTimeMillis();
    }
}
