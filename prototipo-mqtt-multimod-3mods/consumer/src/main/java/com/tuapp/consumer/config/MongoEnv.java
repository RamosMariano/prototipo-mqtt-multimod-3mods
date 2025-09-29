package com.tuapp.consumer.config;

public final class MongoEnv {
    private MongoEnv() {}

    private static String get(String k, String d) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? d : v;
    }

    public static String uri()        { return get("MONGO_URI", "mongodb://mongodb:27017"); }
    public static String db()         { return get("MONGO_DB", "iotdb"); }
    public static String collection() { return get("MONGO_COLLECTION", "readings"); }
}
