package com.tuapp.simulator.mqtt;

import com.google.gson.Gson;
import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.domain.Habitacion;
import com.tuapp.simulator.model.ModeloTermico;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SensorMqttRunner {


    private static final long PUBLISH_INTERVAL_MS =
            Long.parseLong(env("SIM_PUBLISH_INTERVAL_MS", "300000")); // publicar cada 5 min por defecto
    private static final long STEP_MS =
            Long.parseLong(env("SIM_STEP_MS", "1000")); // paso físico de 1 segundo por defecto
    private static final long STEPS_PER_PUB =
            Math.max(1, PUBLISH_INTERVAL_MS / STEP_MS);

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
    private static double envd(String k, double def) {
        try { return Double.parseDouble(env(k, ""+def)); }
        catch (Exception e) { return def; }
    }

    public static void main(String[] args) throws Exception {

        final String BROKER_HOST   = env("BROKER_HOST", "broker-mqtt");
        final String BROKER_PORT   = env("BROKER_PORT", "1883");
        final String BROKER_URL    = "tcp://" + BROKER_HOST + ":" + BROKER_PORT;
        final String CLIENT_ID     = env("CLIENT_ID", "sim-ht-" + System.currentTimeMillis());
        final String USER          = env("MQTT_USER", "");
        final String PASS          = env("MQTT_PASS", "");
        final String TOPIC_OUTDOOR = env("TOPIC_OUTDOOR", "sensors/outdoor/temperature");
        final String TOPIC_OUTDOOR_H = env("TOPIC_OUTDOOR_H", "sensors/outdoor/humidity");
        final String TOPIC_INDOOR  = env("TOPIC_INDOOR",  "sensors/indoor/temperature");
        final int QOS              = Integer.parseInt(env("MQTT_QOS", "0"));
        final boolean RETAIN       = Boolean.parseBoolean(env("MQTT_RETAIN", "false"));
        final String TOPIC_R1_T = env("TOPIC_R1_T", "sensors/room1/temperature");
        final String TOPIC_R1_H  = env("TOPIC_R1_H",  "sensors/room1/humidity");
        final String TOPIC_SW1_S = env("TOPIC_SW1_S", "switches/room1/state");
        final String TOPIC_SW1_P  = env("TOPIC_SW1_P",  "switches/room1/power");
        final String TOPIC_SW1_E = env("TOPIC_SW1_E", "switches/room1/energy");


        final double TEMP_EXTERIOR = envd("TEMP_EXTERIOR", 8.0);
        final double TEMP_INICIAL  = envd("TEMP_INICIAL", 25.0);
        final double SETPOINT      = envd("SETPOINT", 21.0);
        final double HISTERESIS    = envd("HISTERESIS", 3.0);
        final double C             = envd("CAPACIDAD_TERMICA", 1_600_000.0); // J/K
        final double UA            = envd("UA", 75.0);                       // W/K
        final double PIN_W         = envd("POT_TERMICA", 1600.0);            // W térmicos
        final double PEL_W         = envd("POT_ELECTRICA", 1067.0);          // W eléctricos (info/log)

        // sstado inicial
        Habitacion hab = new Habitacion(TEMP_INICIAL);
        ModeloTermico modelo = new ModeloTermico(C, UA);
        Calefaccion cal = new Calefaccion(PIN_W, PEL_W, C);
        Gson gson = new Gson();

        try (MqttPublisher pub = new MqttPublisher(BROKER_URL, CLIENT_ID, USER, PASS)) {
            pub.connect();
            System.out.println("Conectado a MQTT en " + BROKER_URL);

            boolean calefOn = false;
            long step = 0;

            while (true) {
                long ts = Instant.now().toEpochMilli();

                // Control ON/OFF
                double Tin = hab.getTemperaturaInterior();
                if (Tin <= (SETPOINT - HISTERESIS)) calefOn = true;
                else if (Tin >= SETPOINT)           calefOn = false;

                double Pin = calefOn ? cal.getPotenciaTermica() : 0.0;

                // fisica con paso de STEP_MS (1s por defecto)
                double newTin = modelo.paso(Tin, TEMP_EXTERIOR, Pin, STEP_MS / 1000.0);
                hab.setTemperaturaInterior(newTin);

                // publicar cada STEPS_PER_PUB pasos
                if (step % STEPS_PER_PUB == 0) {

                    Map<String, Object> outMsgTemp = new HashMap<>();
                    outMsgTemp.put("deviceId", "outdoor-ht");
                    outMsgTemp.put("temperature", TEMP_EXTERIOR);
                    outMsgTemp.put("unit", "C");
                    outMsgTemp.put("ts", ts);
                    Map<String, Object> outMsgHum = new HashMap<>();
                    outMsgHum.put("deviceId", "outdoor-ht");
                    outMsgHum.put("humidity", "60");
                    outMsgHum.put("unit", "%");
                    outMsgHum.put("ts", ts);

                    Map<String, Object> inMsgTemp = new HashMap<>();
                    inMsgTemp.put("deviceId", "indoor-ht");
                    inMsgTemp.put("temperature", Math.round(newTin * 10.0) / 10.0);
                    inMsgTemp.put("unit", "C");
                    inMsgTemp.put("ts", ts);
                    Map<String, Object> inMsgHum = new HashMap<>();
                    inMsgHum.put("deviceId", "indoor-ht");
                    inMsgHum.put("humidity", "60");
                    inMsgHum.put("unit", "%");
                    inMsgHum.put("ts", ts);

                    Map<String, Object> inSwMsgState = new HashMap<>();
                    inSwMsgState.put("deviceId", "indoor-sw");
                    inSwMsgState.put("state",calefOn ? "ON" : "OFF" );
                    inSwMsgState.put("ts", ts);
                    Map<String, Object> inSwMsgPower = new HashMap<>();
                    inSwMsgPower.put("deviceId", "indoor-sw");
                    inSwMsgPower.put("power",cal.getPotenciaTermicaW() );
                    inSwMsgPower.put("unit", "W");
                    inSwMsgPower.put("ts", ts);
                    Map<String, Object> inSwMsgEnergy = new HashMap<>();
                    inSwMsgEnergy.put("deviceId", "indoor-sw");
                    inSwMsgEnergy.put("energy",cal.getConsumoElectricoW());
                    inSwMsgEnergy.put("unit", "Wh");
                    inSwMsgEnergy.put("ts", ts);





                    /*
                    Map<String, Object> inMsg = new HashMap<>();
                    inMsg.put("sensorId", "indoor-ht");
                    inMsg.put("type", "temperature");
                    inMsg.put("unit", "C");
                    inMsg.put("value", Math.round(newTin * 10.0) / 10.0);
                    inMsg.put("ts", ts);

                     */

                    pub.publishJson(TOPIC_OUTDOOR, gson.toJson(outMsgTemp), QOS, RETAIN);
                    pub.publishJson(TOPIC_OUTDOOR_H, gson.toJson(outMsgHum), QOS, RETAIN);

                    pub.publishJson(TOPIC_R1_T,  gson.toJson(inMsgTemp),  QOS, RETAIN);
                    pub.publishJson(TOPIC_R1_H,  gson.toJson(inMsgHum),  QOS, RETAIN);

                    pub.publishJson(TOPIC_SW1_S,  gson.toJson(inSwMsgState),  QOS, RETAIN);
                    pub.publishJson(TOPIC_SW1_P,  gson.toJson(inSwMsgPower),  QOS, RETAIN);
                    pub.publishJson(TOPIC_SW1_E,  gson.toJson(inSwMsgEnergy),  QOS, RETAIN);

                    System.out.printf("OUT %.1f°C | IN %.1f°C | calef:%s%n",
                            TEMP_EXTERIOR, newTin, calefOn ? "ON" : "OFF");
                }

                step++;
                Thread.sleep(STEP_MS);
            }
        }
    }
}
