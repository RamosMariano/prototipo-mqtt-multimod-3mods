package com.tuapp.api.dto;

import java.time.Instant;

public class EnergiaDTO {
    private double value; // type: number, format: float
    private String unit;  // type: string, example: "kWh"
    private Instant ts;   // type: string, format: date-time (Timestamp del inicio del período de 24h)

    public EnergiaDTO(double value, String unit, Instant ts) {
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