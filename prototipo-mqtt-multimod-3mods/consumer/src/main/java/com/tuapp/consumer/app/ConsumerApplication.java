package com.tuapp.consumer.app;

import com.tuapp.consumer.config.Env;
import com.tuapp.consumer.db.MongoService;
import com.tuapp.consumer.mqtt.MqttSubscriber;
import com.tuapp.consumer.service.DecisionService;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ConsumerApplication {
    public static void main(String[] args) throws MqttException, InterruptedException {
        Env env = Env.fromEnv();
        System.out.printf("[consumer] broker=%s:%d setpoint=%.1f%n",
                env.host(), env.port(), env.setpoint());

        // Servicio de reglas/decisiones (lo dejamos listo, aunque no lo usemos aún)
        DecisionService decision = new DecisionService(env.setpoint());

        // Servicio Mongo (try-with-resources asegura cierre)
        try (MongoService mongo = new MongoService()) {

            // Subscriber: ahora solo formato NUEVO + consolidación por topic
            MqttSubscriber subscriber = new MqttSubscriber(env, decision, mongo);
            subscriber.start();

            System.out.println("[consumer] listo. suscripto a sensors/+/+ y switches/+/+. esperando mensajes…");

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
