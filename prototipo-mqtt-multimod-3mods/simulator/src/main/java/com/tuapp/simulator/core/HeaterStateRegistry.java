package com.tuapp.simulator.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Estado ON/OFF del heater por roomId (default OFF). */
public class HeaterStateRegistry {
    private static final Map<String, AtomicBoolean> MAP = new ConcurrentHashMap<>();

    public static boolean isOn(String roomId) {
        return MAP.computeIfAbsent(roomId, k -> new AtomicBoolean(false)).get();
    }

    public static void set(String roomId, boolean on) {
        MAP.computeIfAbsent(roomId, k -> new AtomicBoolean(false)).set(on);
    }
}
