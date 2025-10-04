package com.tuapp.consumer.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.tuapp.consumer.config.MongoEnv;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;

/**
 * Servicio Mongo:
 * - Crea índice único por topic.
 * - Upsert por topic + push del payload con límite de 110000 elementos.
 */
public class MongoService implements AutoCloseable {
    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> readings;

    public MongoService() {
        this.client   = MongoClients.create(MongoEnv.uri());
        this.db       = client.getDatabase(MongoEnv.db());
        this.readings = db.getCollection(MongoEnv.collection());

        // Índice único por topic (idempotente)
        this.readings.createIndex(
                Indexes.ascending("topic"),
                new IndexOptions().name("uniq_topic").unique(true)
        );

        // (Opcional) índice por lastTs para ordenar por recientes
        // this.readings.createIndex(Indexes.descending("lastTs"),
        //        new IndexOptions().name("idx_lastTs"));
    }

    /**
     * Upsert por topic y push a "raws" con $slice para mantener los últimos 110000 elementos.
     */
    public void appendByTopic(String topic, String rawJson, long ts, long receivedAt) {
        Document payloadDoc = Document.parse(rawJson);

        Document rawEntry = new Document("ts", ts)
                .append("receivedAt", receivedAt)
                .append("payload", payloadDoc);

        Bson filter = eq("topic", topic);

        // $each + $slice para limitar el tamaño a 110000 (últimos)
        Bson pushUpdate = push("raws",
                new Document("$each", List.of(rawEntry))
                        .append("$slice", -110000)
        );

        Bson updates = combine(
                setOnInsert("topic", topic),
                pushUpdate,
                set("receivedAt", receivedAt),
                set("lastTs", ts),
                inc("count", 1)
        );

        readings.updateOne(filter, updates, new UpdateOptions().upsert(true));
    }

    @Override public void close() {
        if (client != null) client.close();
    }
}
