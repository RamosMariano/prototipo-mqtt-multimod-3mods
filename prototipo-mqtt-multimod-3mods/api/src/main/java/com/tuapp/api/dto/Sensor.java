package com.tuapp.api.dto;

public class Sensor {
    private String id; // type: string, example: "1"

    public Sensor(String id) { this.id = id; }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}