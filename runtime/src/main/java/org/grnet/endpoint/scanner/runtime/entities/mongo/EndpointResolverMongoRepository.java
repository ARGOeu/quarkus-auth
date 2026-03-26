package org.grnet.endpoint.scanner.runtime.entities.mongo;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolverRepository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class EndpointResolverMongoRepository implements EndpointResolverRepository {

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;


//    @Override
//    public List<EndpointResolver> findAll() {
//        return getCollectionByClass(EndpointResolver.class)
//                .find()
//                .into(new ArrayList<>());
    public List<EndpointResolver> findAllEndpointResolver() {
        return List.of();
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

    private <T> MongoCollection<T> getCollectionByClass(Class<T> clazz){
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName(), clazz);
    }

    private MongoCollection<Document> getCollection(Class<?> clazz){
        return mongoClient.getDatabase(database).getCollection(clazz.getSimpleName());
    }
    @Override
    public void delete(Long id) {
    }


    @Override
    public void update(EndpointResolver entity) {
    }

    @Override
    public EndpointResolver findById(Long id) {
        return  null;
    }
}

