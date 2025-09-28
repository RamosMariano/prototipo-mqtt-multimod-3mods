package com.tuapp.consumer.service;

public class DecisionService {
    private final double setpoint;
    public DecisionService(double setpoint) { this.setpoint = setpoint; }
    public double setpoint() { return setpoint; }

    /** Regla simple de ejemplo. Ajusta a tu gusto. */
    public boolean shouldHeatOn(double tempIndoor, double tempOutdoor) {
        return tempIndoor < setpoint && tempOutdoor <= setpoint + 5.0;
    }
}
