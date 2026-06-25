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
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.APISetting;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Entitlement;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Setting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class SettingCodec implements CollectibleCodec<Setting> {

    private final Codec<Document> documentCodec;

    public SettingCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public Setting generateIdIfAbsentFromDocument(Setting setting) {
        if (!documentHasId(setting)) {
            setting.setId(new ObjectId().toString());
        }
        return setting;
    }

    @Override
    public boolean documentHasId(Setting setting) {
        return setting.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(Setting setting) {
        return new BsonString(setting.getId());
    }

    @Override
    public Setting decode(BsonReader bsonReader, DecoderContext decoderContext) {
        var document = documentCodec.decode(bsonReader, decoderContext);
        var setting = new Setting();
        setting.setId(document.getString("_id"));
        setting.setValue(document.getString("value"));
        setting.setKey(APISetting.valueOf(document.getString("key")));
        return setting;
    }

    @Override
    public void encode(BsonWriter bsonWriter, Setting setting, EncoderContext encoderContext) {

        var doc = new Document();
        doc.put("_id", setting.getId());
        doc.put("value", setting.getValue());
        doc.put("key", setting.getKey());

        documentCodec.encode(bsonWriter, doc, encoderContext);
    }

    @Override
    public Class<Setting> getEncoderClass() {
        return Setting.class;
    }
}
