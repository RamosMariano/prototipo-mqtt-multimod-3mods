package com.tuapp.consumer.mqtt;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.db.MongoService;
import com.tuapp.consumer.model.SensorReading;
import com.tuapp.consumer.service.DecisionService;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class MqttSubscriber implements MqttCallback {
    private final Env env;
    private final DecisionService decision;
    private final MongoService mongo;
    private final Gson gson = new Gson();

    private final AtomicReference<Double> lastOut = new AtomicReference<>(null);
    private final AtomicReference<Double> lastIn  = new AtomicReference<>(null);

    private MqttClient client;

    public MqttSubscriber(Env env, DecisionService decision, MongoService mongo) {
        this.env = env;
        this.decision = decision;
        this.mongo = mongo;
    }

    public void start() throws MqttException {
        client = MqttClientFactory.newClient(env);
        client.setCallback(this);
        client.connect();
        client.subscribe(env.topicOutdoor());
        client.subscribe(env.topicIndoor());
        System.out.printf("[consumer] subscribed: %s, %s @ %s%n",
                env.topicOutdoor(), env.topicIndoor(), env.brokerUrl());
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("[consumer] conexión perdida: " + (cause != null ? cause.getMessage() : ""));
    }

    @Override
    public void messageArrived(String topic, MqttMessage msg) {
        String payload = new String(msg.getPayload(), StandardCharsets.UTF_8).trim();

        // 1) Intentar parsear como JSON del simulador
        SensorReading reading = null;
        try {
            reading = gson.fromJson(payload, SensorReading.class);
        } catch (JsonSyntaxException ignore) {
            // si no es JSON, más abajo intentamos como numérico "legacy"
        }

        Double numericValue = null;

        if (reading != null && reading.value != null) {
            numericValue = reading.value;
        } else {
            // 2) Soporte legacy: payload numérico plano ("23.4")
            try {
                numericValue = Double.parseDouble(payload);
            } catch (NumberFormatException ex) {
                System.out.printf("[consumer] mensaje no numérico en %s: '%s'%n", topic, payload);
            }
        }

        // 3) Si tenemos un valor numérico, actualizar buffers y ejecutar decisión
        if (numericValue != null) {
            if (topic.equals(env.topicOutdoor())) {
                lastOut.set(numericValue);
                System.out.printf("[OUTDOOR] %.2f °C%n", numericValue);
            } else if (topic.equals(env.topicIndoor())) {
                lastIn.set(numericValue);
                System.out.printf("[INDOOR ] %.2f °C%n", numericValue);
            }

            Double in = lastIn.get(), out = lastOut.get();
            if (in != null && out != null) {
                boolean heatOn = decision.shouldHeatOn(in, out);
                System.out.printf("Setpoint=%.1f → %s (in=%.2f, out=%.2f)%n",
                        decision.setpoint(), heatOn ? "ENCENDER" : "APAGAR", in, out);
            }
        }

        // 4) Persistencia en Mongo: normalizamos a SensorReading y guardamos
        try {
            SensorReading toSave;
            if (reading != null) {
                toSave = reading;
            } else {
                // construir desde legacy numérico (si tampoco hubo number, igual guardamos el raw)
                toSave = new SensorReading();
                toSave.type = "temperature";
                toSave.unit = "C";
                toSave.value = numericValue; // puede quedar null si era texto inválido
                // sensorId lo desconocemos en legacy; lo dejamos null
                // ts del simulador no lo tenemos en legacy; lo dejamos null
            }

            // enriquecer siempre
            toSave.topic = topic;
            toSave.receivedAt = Instant.now().toEpochMilli();

            // si el payload era JSON pero algún campo no venía, igual se guarda tal cual
            Document doc = (payload.startsWith("{") && payload.endsWith("}"))
                    ? Document.parse(gson.toJson(toSave))
                    : Document.parse(gson.toJson(toSave));

            // Adjuntar el payload crudo por trazabilidad (útil para debugging)
            doc.put("raw", payload);

            mongo.insert(doc);

            System.out.printf("[consumer] guardado en Mongo OK topic=%s value=%s sensor=%s%n",
                    toSave.topic, String.valueOf(toSave.value), toSave.sensorId);
        } catch (Exception e) {
            System.err.printf("[consumer] error al persistir en Mongo. topic=%s payload=%s err=%s%n",
                    topic, payload, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }
}
