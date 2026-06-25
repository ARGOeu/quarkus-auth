package org.grnet.endpoint.scanner.runtime.repositories.mongo;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.repositories.ResourceAuthorizationRepository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ResourceAuthorizationMongoRepository implements ResourceAuthorizationRepository {

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;


    @Override
    public List<ResourceAuthorization> findAll() {
        return getCollectionByClass(ResourceAuthorization.class).find().into(new ArrayList<>());
    }


    @Override
    public List<ResourceAuthorization> list(String column, String id) {
        return getCollectionByClass(ResourceAuthorization.class)
                .find(and(
                        eq(column, id)
                )).into(new ArrayList<>());
    }

    @Override
    public void create(ResourceAuthorization entity) {
        getCollectionByClass(ResourceAuthorization.class).insertOne(entity);
    }

    @Override
    public void delete(Long id) {
        getCollectionByClass(ResourceAuthorization.class).deleteOne(eq("_id", id));
    }

    @Override
    public void update(Long id, String rule) {

        getCollectionByClass(ResourceAuthorization.class).updateOne(
                Filters.eq("_id", id),
                Updates.set("rule", rule)
        );
    }

    @Override
    public ResourceAuthorization findById(Long id) {
        return getCollectionByClass(ResourceAuthorization.class)
                .find(and(
                        eq("_id", id)
                )).first();
    }

    private <T> MongoCollection<T> getCollectionByClass(Class<T> clazz){
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName(), clazz);
    }

    private MongoCollection<Document> getCollection(Class<?> clazz) {
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName());
    }
}

