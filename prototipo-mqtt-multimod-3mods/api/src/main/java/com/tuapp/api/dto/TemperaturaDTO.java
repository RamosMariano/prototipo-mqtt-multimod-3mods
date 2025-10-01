package com.tuapp.api.dto;

import java.time.Instant;
// Jackson JSON will handle the serialization of Instant to the required date-time string format

public class TemperaturaDTO {
    private double value; // type: number, format: float
    private String unit;  // type: string, example: "°C"
    private Instant ts;   // type: string, format: date-time

    // Constructor, Getters, and Setters are required by Jackson
    
    public TemperaturaDTO(double value, String unit, Instant ts) {
        this.value = value;
        this.unit = unit;
        this.ts = ts;
    }

    // Getters and Setters
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
}