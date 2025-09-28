package com.tuapp.consumer.config;

public record Env(String host, int port, String topicOutdoor, String topicIndoor, double setpoint) {
    public static Env fromEnv() {
        String host = get("BROKER_HOST", "broker-mqtt");
        int port = Integer.parseInt(get("BROKER_PORT", "1883"));
        String out = get("TOPIC_OUTDOOR", "sensors/outdoor/temperature");
        String in  = get("TOPIC_INDOOR",  "sensors/indoor/temperature");
        double sp  = Double.parseDouble(get("SETPOINT", "21"));
        return new Env(host, port, out, in, sp);
    }
    private static String get(String k, String d) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? d : v;
    }
    public String brokerUrl() { return "tcp://" + host + ":" + port; }
}
