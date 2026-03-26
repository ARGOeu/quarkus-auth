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
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class EndpointResolverCodec implements CollectibleCodec<EndpointResolver> {

    private final Codec<Document> documentCodec;

    public EndpointResolverCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
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
    public EndpointResolver decode(BsonReader bsonReader, DecoderContext decoderContext) {

        var document = documentCodec.decode(bsonReader, decoderContext);
        var endpointResolver = new EndpointResolver();
        endpointResolver.setId(document.getLong("_id"));
        endpointResolver.setSecuredEndpointId(document.getString("secured_endpoint_id"));
        endpointResolver.setResource(document.getString("resource"));
        var date = (Date) document.get("created_at");
        LocalDateTime localDateTime;
        if(Objects.isNull(date)){
            localDateTime = null;
        } else {

            localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        endpointResolver.setCreatedAt(localDateTime);
        endpointResolver.setMappedField(document.getString("mapped_field"));
        endpointResolver.setOriginalField(document.getString("original_field"));

        return endpointResolver;
    }

    @Override
    public void encode(BsonWriter bsonWriter, EndpointResolver endpointResolver, EncoderContext encoderContext) {

        var doc = new Document();
        doc.put("_id", endpointResolver.getId());
        doc.put("resource", endpointResolver.getResource());
        doc.put("created_at", endpointResolver.getCreatedAt());
        doc.put("secured_endpoint_id", endpointResolver.getSecuredEndpointId());
        doc.put("mapped_field", endpointResolver.getMappedField());
        doc.put("original_field", endpointResolver.getOriginalField());
        documentCodec.encode(bsonWriter, doc, encoderContext);
    }

    @Override
    public Class<EndpointResolver> getEncoderClass() {
        return EndpointResolver.class;
    }

    private long generateNewId() {
        return UUID.randomUUID().getMostSignificantBits();
    }
}
