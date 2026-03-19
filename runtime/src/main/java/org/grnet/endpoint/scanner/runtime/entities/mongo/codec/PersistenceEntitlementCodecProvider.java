package org.grnet.endpoint.scanner.runtime.entities.mongo.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Actor;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.ActorEntitlements;
import org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence.Entitlement;

public class PersistenceEntitlementCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Actor.class)) {
            return (Codec<T>) new ActorCodec();
        } else if (clazz.equals(Entitlement.class)) {
            return (Codec<T>) new EntitlementCodec();
        } else if (clazz.equals(ActorEntitlements.class)) {
            return (Codec<T>) new ActorEntitlementsCodec();
        }
        return null;
    }
}
