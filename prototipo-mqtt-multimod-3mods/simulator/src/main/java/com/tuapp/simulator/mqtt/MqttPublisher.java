package com.tuapp.simulator.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import java.nio.charset.StandardCharsets;

public class MqttPublisher implements AutoCloseable {
    private final String brokerUrl, clientId, username, password;
    private IMqttClient client;

    public MqttPublisher(String brokerUrl, String clientId, String username, String password) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }

    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        if (username != null && !username.isBlank()) opts.setUserName(username);
        if (password != null && !password.isBlank()) opts.setPassword(password.toCharArray());
        client.connect(opts);
    }

    public boolean isConnected() { return client != null && client.isConnected(); }

    public void publishJson(String topic, String json, int qos, boolean retain) throws MqttException {
        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(qos);
        msg.setRetained(retain);
        client.publish(topic, msg);
    }

    @Override public void close() {
        try { if (client != null && client.isConnected()) client.disconnect(); } catch (Exception ignored) {}
        try { if (client != null) client.close(); } catch (Exception ignored) {}
    }
}