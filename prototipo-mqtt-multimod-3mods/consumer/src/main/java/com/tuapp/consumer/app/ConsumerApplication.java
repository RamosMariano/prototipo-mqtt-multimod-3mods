package com.tuapp.consumer.app;

import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.db.MongoService;
import com.tuapp.consumer.mqtt.MqttSubscriber;
import com.tuapp.consumer.service.DecisionService;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConsumerApplication {
    public static void main(String[] args) throws MqttException, InterruptedException {
        Env env = Env.fromEnv();
        System.out.printf("[consumer] broker=%s:%d topics=[%s,%s] setpoint=%.1f%n",
                env.host(), env.port(), env.topicOutdoor(), env.topicIndoor(), env.setpoint());

        // Servicio de reglas/decisiones
        DecisionService decision = new DecisionService(env.setpoint());

        // Servicio Mongo (try-with-resources asegura cierre)
        try (MongoService mongo = new MongoService()) {

            // Subscriber ahora recibe también MongoService
            MqttSubscriber subscriber = new MqttSubscriber(env, decision, mongo);
            subscriber.start();

            System.out.println("[consumer] listo. esperando mensajes…");

            // Hook de cierre ordenado (por si recibís SIGTERM en Docker, etc.)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    mongo.close();
                } catch (Exception ignored) {}
                System.out.println("[consumer] shutdown completo.");
            }));

            // Mantener vivo el hilo principal
            Thread.currentThread().join();
        }
    }
}
