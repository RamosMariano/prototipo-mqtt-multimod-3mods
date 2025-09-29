package com.tuapp.consumer.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tuapp.consumer.config.MongoEnv;
import org.bson.Document;

public class MongoService implements AutoCloseable {
    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> readings;

    public MongoService() {
        this.client   = MongoClients.create(MongoEnv.uri());
        this.db       = client.getDatabase(MongoEnv.db());
        this.readings = db.getCollection(MongoEnv.collection());
    }

    public void insert(Document doc) {
        readings.insertOne(doc);
    }

    @Override public void close() {
        if (client != null) client.close();
    }
}
