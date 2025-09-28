package com.tuapp.consumer.app;

import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.mqtt.MqttSubscriber;
import com.tuapp.consumer.service.DecisionService;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConsumerApplication {
    public static void main(String[] args) throws MqttException {
        Env env = Env.fromEnv();
        System.out.printf("[consumer] broker=%s:%d topics=[%s,%s] setpoint=%.1f%n",
                env.host(), env.port(), env.topicOutdoor(), env.topicIndoor(), env.setpoint());

        DecisionService decision = new DecisionService(env.setpoint());
        MqttSubscriber subscriber = new MqttSubscriber(env, decision);
        subscriber.start();

        System.out.println("[consumer] listo. esperando mensajes…");
    }
}
