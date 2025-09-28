package com.tuapp.consumer;

public class ConsumerMain {
  public static void main(String[] args) throws Exception {
    String host = System.getenv().getOrDefault("BROKER_HOST", "localhost");
    String port = System.getenv().getOrDefault("BROKER_PORT", "1883");
    System.out.println("[consumer] arrancando... broker=" + host + ":" + port);
    // TODO: Reemplazar con tu lógica de consumo MQTT (Paho).
  }
}
