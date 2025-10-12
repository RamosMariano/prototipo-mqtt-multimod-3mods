package com.tuapp.simulator.mqtt;

import com.google.gson.Gson;
import com.tuapp.simulator.core.HeaterStateRegistry;
import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.domain.Habitacion;
import com.tuapp.simulator.model.ModeloTermico;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensorMqttRunner {

    // ===== Helpers ENV =====
    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
    private static double envd(String k, double def) {
        try { return Double.parseDouble(env(k, ""+def)); } catch (Exception e) { return def; }
    }
    private static long envl(String k, long def) {
        try { return Long.parseLong(env(k, ""+def)); } catch (Exception e) { return def; }
    }
    private static int envi(String k, int def) {
        try { return Integer.parseInt(env(k, ""+def)); } catch (Exception e) { return def; }
    }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    // Acumuladores por room (energy Wh)
    private static final Map<String, Double> energyWhAcc = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        // ===== Room =====
        final String ROOM_ID = env("SIM_ROOM_ID", "room1");

        // ===== MQTT =====
        final String BROKER_HOST   = env("BROKER_HOST", "broker-mqtt");
        final String BROKER_PORT   = env("BROKER_PORT", "1883");
        final String BROKER_URL    = "tcp://" + BROKER_HOST + ":" + BROKER_PORT;
        final String CLIENT_ID     = env("CLIENT_ID", "sim-ht-" + System.currentTimeMillis());
        final String USER          = env("MQTT_USER", "");
        final String PASS          = env("MQTT_PASS", "");
        final int    QOS           = envi("MQTT_QOS", 0);
        final boolean RETAIN       = Boolean.parseBoolean(env("MQTT_RETAIN", "false"));

        // Topics (parametrizados por ROOM_ID; si exportás TOPIC_* se respetan)
        final String TOPIC_OUTDOOR   = env("TOPIC_OUTDOOR",   "sensors/outdoor/temperature");
        final String TOPIC_OUTDOOR_H = env("TOPIC_OUTDOOR_H", "sensors/outdoor/humidity");
        final String TOPIC_INDOOR_T  = env("TOPIC_R1_T",      "sensors/" + ROOM_ID + "/temperature");
        final String TOPIC_INDOOR_H  = env("TOPIC_R1_H",      "sensors/" + ROOM_ID + "/humidity");
        final String TOPIC_SW_STATE  = env("TOPIC_SW1_S",     "switches/" + ROOM_ID + "/state");
        final String TOPIC_SW_POWER  = env("TOPIC_SW1_P",     "switches/" + ROOM_ID + "/power");
        final String TOPIC_SW_ENERGY = env("TOPIC_SW1_E",     "switches/" + ROOM_ID + "/energy");

        // ===== Simulación (time warp) =====
        final long   STEP_MS_REAL            = envl("SIM_STEP_MS", 1000L);          // cuánto duerme el hilo real
        final double SIM_SPEED               = envd("SIM_SPEED", 60.0);             // 60×: 1s real = 60s simulados
        final long   PUBLISH_INTERVAL_MS_SIM = envl("SIM_PUBLISH_INTERVAL_MS", 300_000L); // 5 min simulados

        // ===== Modelo térmico =====
        final double TEMP_EXTERIOR = envd("TEMP_EXTERIOR", 8.0);
        final double TEMP_INICIAL  = envd("TEMP_INICIAL", 25.0);
        final double C             = envd("CAPACIDAD_TERMICA", 1_600_000.0); // J/K
        final double UA            = envd("UA", 75.0);                       // W/K
        final double PIN_W         = envd("POT_TERMICA", 1600.0);            // W térmicos
        final double PEL_W         = envd("POT_ELECTRICA", 1067.0);          // W eléctricos (consumo)

        // ===== Estado inicial =====
        Habitacion hab    = new Habitacion(TEMP_INICIAL);
        ModeloTermico mod = new ModeloTermico(C, UA);
        Calefaccion cal   = new Calefaccion(PIN_W, PEL_W, C);
        Gson gson         = new Gson();

        // Reloj SIMULADO (no real): arranca desde ahora y avanza acelerado
        long simTs = Instant.now().toEpochMilli();
        long accSimMs = 0;

        // energía acumulada por room (inicial)
        energyWhAcc.putIfAbsent(ROOM_ID, 0.0);

        try (MqttPublisher pub = new MqttPublisher(BROKER_URL, CLIENT_ID, USER, PASS)) {
            pub.connect();
            System.out.printf("Conectado a MQTT en %s | SIM_SPEED=%.2f× | publica cada %d ms (simulados)%n",
                    BROKER_URL, SIM_SPEED, PUBLISH_INTERVAL_MS_SIM);

            while (true) {
                // === ON/OFF desde endpoint (NO termostato) ===
                boolean calefOn = HeaterStateRegistry.isOn(ROOM_ID);

                // Potencia térmica de entrada (si ON)
                double PinTermica = calefOn ? cal.getPotenciaTermica() : 0.0;

                // ---- Paso de física con dt SIMULADO ----
                double dtSecondsSim = (STEP_MS_REAL / 1000.0) * SIM_SPEED; // ej: 1s real * 60 = 60s simulados
                double Tin0 = hab.getTemperaturaInterior();
                double Tin1 = mod.paso(Tin0, TEMP_EXTERIOR, PinTermica, dtSecondsSim);
                hab.setTemperaturaInterior(Tin1);

                // ---- Avanzar reloj SIMULADO ----
                long deltaSimMs = Math.round(STEP_MS_REAL * SIM_SPEED);
                simTs += deltaSimMs;
                accSimMs += deltaSimMs;

                // ---- Integrar energía (Wh) con delta SIMULADO ----
                double powerW = calefOn ? cal.getConsumoElectrico() : 0.0; // instantáneo (eléctrico)
                double deltaWh = powerW * (deltaSimMs / 3600000.0);
                double energyWh = energyWhAcc.compute(ROOM_ID, (k, v) -> (v == null ? 0.0 : v) + deltaWh);

                // ---- Publicar cada intervalo SIMULADO ----
                if (accSimMs >= PUBLISH_INTERVAL_MS_SIM) {
                    long ts = simTs; // usar timestamp SIMULADO en el payload

                    // -------- Sensores ambiente --------
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

                    // -------- Switch: state / power / energy --------
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

                    System.out.printf("[PUB] ts=%s | OUT %.1f°C | IN %.1f°C | calef:%s | P=%.1f W | E=%.1f Wh%n",
                            Instant.ofEpochMilli(ts), TEMP_EXTERIOR, Tin1, (calefOn ? "ON" : "OFF"), powerW, energyWh);

                    accSimMs = 0; // reiniciar acumulador simulado
                }

                // ---- Paso real (control de CPU) ----
                Thread.sleep(STEP_MS_REAL);
            }
        }
    }
}
