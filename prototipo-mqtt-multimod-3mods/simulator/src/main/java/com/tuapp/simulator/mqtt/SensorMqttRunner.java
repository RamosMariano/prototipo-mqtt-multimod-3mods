package com.tuapp.simulator.mqtt;

import com.google.gson.Gson;
import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.domain.Habitacion;
import com.tuapp.simulator.model.ModeloTermico;
import com.tuapp.simulator.core.HeaterStateRegistry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensorMqttRunner {

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
    private static double envd(String k, double def) {
        try { return Double.parseDouble(env(k, ""+def)); }
        catch (Exception e) { return def; }
    }

    // Frecuencias
    private static final long PUBLISH_INTERVAL_MS =
            Long.parseLong(env("SIM_PUBLISH_INTERVAL_MS", "300000")); // 5 min
    private static final long STEP_MS =
            Long.parseLong(env("SIM_STEP_MS", "1000")); // 1s
    private static final long STEPS_PER_PUB =
            Math.max(1, PUBLISH_INTERVAL_MS / STEP_MS);

    // Acumuladores por "roomId" (aquí usamos "room1")
    private static final Map<String, Double> energyWhAcc = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastTs = new ConcurrentHashMap<>();

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    /** Integra energía Wh = ∫(W * dt[h]) */
    private static double integrateEnergyWh(String roomId, double instantW, long nowMs) {
        long prev = lastTs.getOrDefault(roomId, nowMs);
        lastTs.put(roomId, nowMs);
        long deltaMs = Math.max(0, nowMs - prev);

        double deltaWh = instantW * (deltaMs / 3600000.0); // (W * ms) / (1000*3600)
        double newAcc = energyWhAcc.getOrDefault(roomId, 0.0) + deltaWh;
        energyWhAcc.put(roomId, newAcc);
        return newAcc;
    }

    public static void main(String[] args) throws Exception {
        final String ROOM_ID = env("SIM_ROOM_ID", "room1");
        final String BROKER_HOST   = env("BROKER_HOST", "broker-mqtt");
        final String BROKER_PORT   = env("BROKER_PORT", "1883");
        final String BROKER_URL    = "tcp://" + BROKER_HOST + ":" + BROKER_PORT;
        final String CLIENT_ID     = env("CLIENT_ID", "sim-ht-" + System.currentTimeMillis());
        final String USER          = env("MQTT_USER", "");
        final String PASS          = env("MQTT_PASS", "");

        // Topics
        final String TOPIC_OUTDOOR   = env("TOPIC_OUTDOOR",   "sensors/outdoor/temperature");
        final String TOPIC_OUTDOOR_H = env("TOPIC_OUTDOOR_H", "sensors/outdoor/humidity");
        final String TOPIC_INDOOR_T  = env("TOPIC_R1_T",      "sensors/"+ROOM_ID+"/temperature");
        final String TOPIC_INDOOR_H  = env("TOPIC_R1_H",      "sensors/"+ROOM_ID+"/humidity");
        final String TOPIC_SW_STATE  = env("TOPIC_SW1_S",     "switches/"+ROOM_ID+"/state");
        final String TOPIC_SW_POWER  = env("TOPIC_SW1_P",     "switches/"+ROOM_ID+"/power");
        final String TOPIC_SW_ENERGY = env("TOPIC_SW1_E",     "switches/"+ROOM_ID+"/energy");

        final int QOS         = Integer.parseInt(env("MQTT_QOS", "0"));
        final boolean RETAIN  = Boolean.parseBoolean(env("MQTT_RETAIN", "false"));

        // Parámetros térmicos
        final double TEMP_EXTERIOR = envd("TEMP_EXTERIOR", 8.0);
        final double TEMP_INICIAL  = envd("TEMP_INICIAL", 25.0);
        final double C             = envd("CAPACIDAD_TERMICA", 1_600_000.0); // J/K
        final double UA            = envd("UA", 75.0);                       // W/K
        final double PIN_W         = envd("POT_TERMICA", 1600.0);            // W térmicos
        final double PEL_W         = envd("POT_ELECTRICA", 1067.0);          // W eléctricos (consumo)

        // Estado inicial del modelo
        Habitacion hab     = new Habitacion(TEMP_INICIAL);
        ModeloTermico mod  = new ModeloTermico(C, UA);
        Calefaccion cal    = new Calefaccion(PIN_W, PEL_W, C);
        Gson gson          = new Gson();


        try (MqttPublisher pub = new MqttPublisher(BROKER_URL, CLIENT_ID, USER, PASS)) {
            pub.connect();
            System.out.println("Conectado a MQTT en " + BROKER_URL);

            long step = 0;

            while (true) {
                long ts = Instant.now().toEpochMilli();

                // === ON/OFF desde endpoint: NO hay termostato ===
                boolean calefOn = HeaterStateRegistry.isOn(ROOM_ID);

                // Potencia térmica que entra al modelo (si ON)
                double PinTermica = calefOn ? cal.getPotenciaTermica() : 0.0;

                // Evolución física 1s (o STEP_MS)
                double Tin0 = hab.getTemperaturaInterior();
                double Tin1 = mod.paso(Tin0, TEMP_EXTERIOR, PinTermica, STEP_MS / 1000.0);
                hab.setTemperaturaInterior(Tin1);

                // Cada N pasos publicamos
                if (step % STEPS_PER_PUB == 0) {

                    // -------- Sensores (ejemplo simple) --------
                    Map<String, Object> outTemp = new HashMap<>();
                    outTemp.put("deviceId", "outdoor-ht");
                    outTemp.put("temperature", TEMP_EXTERIOR);
                    outTemp.put("unit", "C");
                    outTemp.put("ts", ts);

                    Map<String, Object> outHum = new HashMap<>();
                    outHum.put("deviceId", "outdoor-ht");
                    outHum.put("humidity", "60");
                    outHum.put("unit", "%");
                    outHum.put("ts", ts);

                    Map<String, Object> inTemp = new HashMap<>();
                    inTemp.put("deviceId", "indoor-ht");
                    inTemp.put("temperature", Math.round(Tin1 * 10.0) / 10.0);
                    inTemp.put("unit", "C");
                    inTemp.put("ts", ts);

                    Map<String, Object> inHum = new HashMap<>();
                    inHum.put("deviceId", "indoor-ht");
                    inHum.put("humidity", "60");
                    inHum.put("unit", "%");
                    inHum.put("ts", ts);

                    pub.publishJson(TOPIC_OUTDOOR,   gson.toJson(outTemp), QOS, RETAIN);
                    pub.publishJson(TOPIC_OUTDOOR_H, gson.toJson(outHum),  QOS, RETAIN);
                    pub.publishJson(TOPIC_INDOOR_T,  gson.toJson(inTemp),  QOS, RETAIN);
                    pub.publishJson(TOPIC_INDOOR_H,  gson.toJson(inHum),   QOS, RETAIN);

                    // -------- Switch: state/power/energy --------
                    // power = potencia eléctrica instantánea (W)
                    double powerW = calefOn ? cal.getConsumoElectrico() : 0.0;
                    double energyWh = integrateEnergyWh(ROOM_ID, powerW, ts);

                    Map<String, Object> swState = new HashMap<>();
                    swState.put("deviceId", "indoor-sw");
                    swState.put("state", calefOn ? "ON" : "OFF");
                    swState.put("ts", ts);

                    Map<String, Object> swPower = new HashMap<>();
                    swPower.put("deviceId", "indoor-sw");
                    swPower.put("power", round1(powerW)); // W
                    swPower.put("unit", "W");
                    swPower.put("ts", ts);

                    Map<String, Object> swEnergy = new HashMap<>();
                    swEnergy.put("deviceId", "indoor-sw");
                    swEnergy.put("energy", round1(energyWh)); // Wh acumulados
                    swEnergy.put("unit", "Wh");
                    swEnergy.put("ts", ts);

                    pub.publishJson(TOPIC_SW_STATE,  gson.toJson(swState),  QOS, RETAIN);
                    pub.publishJson(TOPIC_SW_POWER,  gson.toJson(swPower),  QOS, RETAIN);
                    pub.publishJson(TOPIC_SW_ENERGY, gson.toJson(swEnergy), QOS, RETAIN);

                    System.out.printf("OUT %.1f°C | IN %.1f°C | calef:%s | P=%.1f W | E=%.1f Wh%n",
                            TEMP_EXTERIOR, Tin1, (calefOn ? "ON" : "OFF"), powerW, energyWh);
                }

                step++;
                Thread.sleep(STEP_MS);
            }
        }
    }
}
