package com.tuapp.api.dto;

import java.util.List;

public class Room {
    private String id;
    private List<Sensor> sensores; // Matches the 'sensores' array schema
    private List<Switch> switches; // Matches the 'switches' array schema

    public Room(String id, List<Sensor> sensores, List<Switch> switches) {
        this.id = id;
        this.sensores = sensores;
        this.switches = switches;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<Sensor> getSensores() { return sensores; }
    public void setSensores(List<Sensor> sensores) { this.sensores = sensores; }
    public List<Switch> getSwitches() { return switches; }
    public void setSwitches(List<Switch> switches) { this.switches = switches; }
}