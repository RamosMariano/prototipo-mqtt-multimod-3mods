package com.tuapp.consumer.mqtt;

import com.tuapp.consumer.config.Env;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttClientFactory {
    public static MqttClient newClient(Env env) throws MqttException {
        return new MqttClient(env.brokerUrl(), MqttClient.generateClientId());
    }
}
