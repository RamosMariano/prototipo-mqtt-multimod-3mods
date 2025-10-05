package com.tuapp.simulator.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.tuapp.simulator.core.HeaterStateRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlHttpServer implements AutoCloseable {
    private final HttpServer server;

    // Endpoint: POST /rooms/{roomId}/heater?on=true|false
    private static final Pattern PATH = Pattern.compile("^/rooms/([^/]+)/heater$");

    public ControlHttpServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/rooms", this::handleRooms);
    }

    public void start() { server.start(); }
    @Override public void close() { server.stop(0); }

    private void handleRooms(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                write(ex, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            Matcher m = PATH.matcher(ex.getRequestURI().getPath());
            if (!m.find()) {
                write(ex, 404, "{\"error\":\"not found\"}");
                return;
            }
            String roomId = m.group(1);
            String qs = ex.getRequestURI().getQuery();
            boolean on = qs != null && qs.matches(".*(?:^|&)on=true(?:&|$).*");
            HeaterStateRegistry.set(roomId, on);
            write(ex, 200, "{\"roomId\":\""+roomId+"\",\"heaterOn\":"+on+"}");
        } catch (Exception e) {
            write(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void write(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
