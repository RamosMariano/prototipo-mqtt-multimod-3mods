package com.tuapp.simulator.mqtt;

import com.google.gson.Gson;
import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.domain.Habitacion;
import com.tuapp.simulator.model.ModeloTermico;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SensorMqttRunner {

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
    private static double envd(String k, double def) {
        try { return Double.parseDouble(env(k, ""+def)); }
        catch (Exception e) { return def; }
    }

    public static void main(String[] args) throws Exception {
        // ====== ENV (con defaults) ======
        final String BROKER_HOST = env("BROKER_HOST", "broker-mqtt");
        final String BROKER_PORT = env("BROKER_PORT", "1883");
        final String BROKER_URL  = "tcp://" + BROKER_HOST + ":" + BROKER_PORT;
        final String CLIENT_ID   = env("CLIENT_ID", "sim-ht-" + System.currentTimeMillis());
        final String USER        = env("MQTT_USER", "");
        final String PASS        = env("MQTT_PASS", "");
        final String TOPIC_OUTDOOR = env("TOPIC_OUTDOOR", "sensors/outdoor/temperature");
        final String TOPIC_INDOOR  = env("TOPIC_INDOOR",  "sensors/indoor/temperature");
        final int QOS = Integer.parseInt(env("MQTT_QOS", "0"));
        final boolean RETAIN = Boolean.parseBoolean(env("MQTT_RETAIN", "false"));

        final double TEMP_EXTERIOR = envd("TEMP_EXTERIOR", 8.0);
        final double TEMP_INICIAL  = envd("TEMP_INICIAL", 25.0);
        final double SETPOINT      = envd("SETPOINT", 21.0);
        final double HISTERESIS    = envd("HISTERESIS", 3.0);
        final double C  = envd("CAPACIDAD_TERMICA", 1_600_000.0); // J/K
        final double UA = envd("UA", 75.0);                       // W/K
        final double PIN_W = envd("POT_TERMICA", 1600.0);         // W térmicos
        final double PEL_W = envd("POT_ELECTRICA", 1067.0);       // W eléctricos (solo info/log)

        Habitacion hab = new Habitacion(TEMP_INICIAL);
        ModeloTermico modelo = new ModeloTermico(C, UA);
        Calefaccion cal = new Calefaccion(PIN_W, PEL_W, C);

        Gson gson = new Gson();

        try (MqttPublisher pub = new MqttPublisher(BROKER_URL, CLIENT_ID, USER, PASS)) {
            pub.connect();
            System.out.println("Conectado a MQTT en " + BROKER_URL);

            boolean calefOn = false;

            while (true) {
                long ts = Instant.now().toEpochMilli();

                double Tin = hab.getTemperaturaInterior();
                if (Tin <= (SETPOINT - HISTERESIS)) calefOn = true;
                else if (Tin >= SETPOINT)           calefOn = false;

                double Pin = calefOn ? cal.getPotenciaTermica() : 0.0;

                double newTin = modelo.paso(Tin, TEMP_EXTERIOR, Pin, 1.0);
                hab.setTemperaturaInterior(newTin);

                Map<String, Object> outMsg = new HashMap<>();
                outMsg.put("sensorId", "outdoor-ht");
                outMsg.put("type", "temperature");
                outMsg.put("unit", "C");
                outMsg.put("value", TEMP_EXTERIOR);
                outMsg.put("ts", ts);

                Map<String, Object> inMsg = new HashMap<>();
                inMsg.put("sensorId", "indoor-ht");
                inMsg.put("type", "temperature");
                inMsg.put("unit", "C");
                inMsg.put("value", Math.round(newTin * 10.0) / 10.0);
                inMsg.put("ts", ts);

                pub.publishJson(TOPIC_OUTDOOR, gson.toJson(outMsg), QOS, RETAIN);
                pub.publishJson(TOPIC_INDOOR,  gson.toJson(inMsg),  QOS, RETAIN);

                System.out.printf("OUT %.1f°C | IN %.1f°C | calef:%s%n",
                        TEMP_EXTERIOR, newTin, calefOn ? "ON" : "OFF");

                Thread.sleep(1000);
            }
        }
    }
}
