package com.tuapp.consumer.mqtt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.db.MongoService;
import com.tuapp.consumer.service.DecisionService;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Subscriber:
 * - Se suscribe a sensors/+/+ y switches/+/+.
 * - Solo formato NUEVO (JSON).
 * - Persiste por topic acumulando payloads en 'raws' (límite 110000).
 */
public class MqttSubscriber implements MqttCallback {
    private final Env env;
    private final DecisionService decision;
    private final MongoService mongo;
    private MqttClient client;

    public MqttSubscriber(Env env, DecisionService decision, MongoService mongo) {
        this.env = env;
        this.decision = decision;
        this.mongo = mongo;
    }

    public void start() throws MqttException {
        this.client = MqttClientFactory.newClient(env);
        this.client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        client.connect(options);

        // Suscripciones NUEVAS
        client.subscribe("sensors/+/+", 1);
        client.subscribe("switches/+/+", 1);
        System.out.println("[consumer] suscripto a: sensors/+/+, switches/+/+");
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[consumer] conexión MQTT perdida: " + (cause != null ? cause.getMessage() : ""));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        long receivedAt = Instant.now().toEpochMilli();

        try {
            // Formato NUEVO: parse JSON
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            long ts = (json.has("ts") && json.get("ts").isJsonPrimitive())
                    ? json.get("ts").getAsLong()
                    : receivedAt;

            // Persistencia: upsert por topic + push con slice 110000
            mongo.appendByTopic(topic, payload, ts, receivedAt);

            System.out.printf("[consumer] appended topic=%s ts=%d%n", topic, ts);

            // (Opcional) disparar reglas
            // decision.evaluateIfNeeded(topic, json);

        } catch (Exception e) {
            System.err.printf("[consumer] error topic=%s payload=%s err=%s%n", topic, payload, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }
}
