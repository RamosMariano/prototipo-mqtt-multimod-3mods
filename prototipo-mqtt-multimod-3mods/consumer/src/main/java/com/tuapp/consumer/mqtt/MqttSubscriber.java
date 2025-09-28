package com.tuapp.consumer.mqtt;

import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.service.DecisionService;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class MqttSubscriber implements MqttCallback {
    private final Env env;
    private final DecisionService decision;
    private final AtomicReference<Double> lastOut = new AtomicReference<>(null);
    private final AtomicReference<Double> lastIn  = new AtomicReference<>(null);
    private MqttClient client;

    public MqttSubscriber(Env env, DecisionService decision) {
        this.env = env; this.decision = decision;
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

    @Override public void connectionLost(Throwable cause) {
        System.out.println("[consumer] conexión perdida: " + (cause != null ? cause.getMessage() : ""));
    }

    @Override public void messageArrived(String topic, MqttMessage msg) {
        String payload = new String(msg.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            double val = Double.parseDouble(payload);
            if (topic.equals(env.topicOutdoor())) {
                lastOut.set(val); System.out.printf("[OUTDOOR] %.2f °C%n", val);
            } else if (topic.equals(env.topicIndoor())) {
                lastIn.set(val);  System.out.printf("[INDOOR ] %.2f °C%n", val);
            }
            Double in = lastIn.get(), out = lastOut.get();
            if (in != null && out != null) {
                boolean heatOn = decision.shouldHeatOn(in, out);
                System.out.printf("Setpoint=%.1f → %s (in=%.2f, out=%.2f)%n",
                        decision.setpoint(), heatOn ? "ENCENDER" : "APAGAR", in, out);
            }
        } catch (NumberFormatException e) {
            System.out.printf("[consumer] mensaje no numérico en %s: '%s'%n", topic, payload);
        }
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) { }
}
