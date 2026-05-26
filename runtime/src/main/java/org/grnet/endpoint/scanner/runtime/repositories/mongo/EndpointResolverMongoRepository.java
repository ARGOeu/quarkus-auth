package org.grnet.endpoint.scanner.runtime.repositories.mongo;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.repositories.EndpointResolverRepository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class EndpointResolverMongoRepository implements EndpointResolverRepository {

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;

    public List<EndpointResolver> findAll() {
        return getCollectionByClass(EndpointResolver.class).find().into(new ArrayList<>());
    }

    @Override
    public List<EndpointResolver> list(String column, String id) {
        return getCollectionByClass(EndpointResolver.class)
                .find(and(
                        eq(column, id)
                )).into(new ArrayList<>());
    }

    @Override
    public void create(EndpointResolver entity) {
        getCollectionByClass(EndpointResolver.class).insertOne(entity);
    }

    @Override
    public void delete(Long id) {
        getCollectionByClass(EndpointResolver.class).deleteOne(eq("_id", id));
    }

    @Override
    public void update(EndpointResolver entity) {
        getCollectionByClass(EndpointResolver.class).replaceOne(
                Filters.eq("_id", entity.getId()),
                entity
        );
    }

    @Override
    public EndpointResolver findById(Long id) {
        return getCollectionByClass(EndpointResolver.class)
                .find(and(
                        eq("_id", id)
                )).first();
    }

    private <T> MongoCollection<T> getCollectionByClass(Class<T> clazz){
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName(), clazz);
    }

    private MongoCollection<Document> getCollection(Class<?> clazz){
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName());
    }
}

