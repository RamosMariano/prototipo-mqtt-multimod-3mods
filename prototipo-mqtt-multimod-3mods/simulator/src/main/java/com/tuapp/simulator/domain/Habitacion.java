package com.tuapp.simulator.domain;

public class Habitacion {

    private double volumenAire;         // m³ (opcional en esta simulación)
    private double energiaPorGrado;     // J/°C (opcional)
    private double u;                   // W/m²·K (opcional)
    private double area;                // m² (opcional)
    private double temperaturaInterior; // °C

    public Habitacion() {}

    public Habitacion(double volumenAire, double energiaPorGrado, double u, double area, double temperaturaInterior) {
        this.volumenAire = volumenAire;
        this.energiaPorGrado = energiaPorGrado;
        this.u = u;
        this.area = area;
        this.temperaturaInterior = temperaturaInterior;
    }

    public Habitacion(double temperaturaInteriorInicial) {
        this.temperaturaInterior = temperaturaInteriorInicial;
    }

    public double getTemperaturaInterior() { return temperaturaInterior; }
    public void setTemperaturaInterior(double temperaturaInterior) { this.temperaturaInterior = temperaturaInterior; }

    public double getVolumenAire() { return volumenAire; }
    public void setVolumenAire(double volumenAire) { this.volumenAire = volumenAire; }

    public double getEnergiaPorGrado() { return energiaPorGrado; }
    public void setEnergiaPorGrado(double energiaPorGrado) { this.energiaPorGrado = energiaPorGrado; }

    public double getU() { return u; }
    public void setU(double u) { this.u = u; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    @Override
    public String toString() {
        return "Habitacion{" +
                "volumenAire=" + volumenAire +
                ", energiaPorGrado=" + energiaPorGrado +
                ", u=" + u +
                ", area=" + area +
                ", temperaturaInterior=" + temperaturaInterior +
                '}';
    }
}