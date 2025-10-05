package com.tuapp.simulator.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import java.nio.charset.StandardCharsets;

public class MqttPublisher implements AutoCloseable {
    private final String brokerUrl, clientId, user, pass;
    private IMqttClient client;

    public MqttPublisher(String brokerUrl, String clientId, String user, String pass) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.user = user;
        this.pass = pass;
    }

    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);
        opts.setKeepAliveInterval(30);
        if (user != null && !user.isBlank()) {
            opts.setUserName(user);
            opts.setPassword(pass != null ? pass.toCharArray() : new char[0]);
        }
        client.connect(opts);
    }

    private void ensureConnected() throws MqttException {
        if (client == null) throw new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);
        if (!client.isConnected()) {
            // intenta reconectar sin tirar la app
            try { client.reconnect(); } catch (MqttException ignored) {}
            if (!client.isConnected()) throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    public void publishJson(String topic, String json, int qos, boolean retain) throws MqttException {
        ensureConnected();
        MqttMessage msg = new MqttMessage(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        msg.setQos(qos);
        msg.setRetained(retain);
        client.publish(topic, msg);
    }

    @Override public void close() {
        try { if (client != null && client.isConnected()) client.disconnect(); } catch (Exception ignored) {}
        try { if (client != null) client.close(); } catch (Exception ignored) {}
    }
}