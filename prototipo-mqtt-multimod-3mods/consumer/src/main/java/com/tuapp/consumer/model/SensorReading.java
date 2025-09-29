package com.tuapp.consumer.model;

public class SensorReading {
    public String unit;
    public String type;
    public Double value;
    public String sensorId;
    public Long ts;

    // enriquecidos al consumir
    public String topic;
    public Long receivedAt;

    public SensorReading() {}
}
