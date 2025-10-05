package com.tuapp.simulator.app;
import com.tuapp.simulator.http.ControlHttpServer;


public class SimulatorMain {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getenv().getOrDefault("SIM_HTTP_PORT","8099"));
    try (ControlHttpServer http = new ControlHttpServer(port)) {
      http.start();
      // Deja correr tu simulador MQTT como ya lo hacías:
      com.tuapp.simulator.mqtt.SensorMqttRunner.main(args);
    }
  }
}