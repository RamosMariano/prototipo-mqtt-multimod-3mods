package com.tuapp.api.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MongoRepo extends MongoRepository<MongoReading, String> {
    // Buscar por topic (ej: "sensors/room-1/1")
    Optional<MongoReading> findByTopic(String topic);
}
