package com.tuapp.simulator.model;

/**
 * Modelo térmico (RC): C dT/dt = P_in - UA (T - T_out)
 * T(t) = Tss + (T0 - Tss) * exp(-t/tau), con Tss = T_out + P_in/UA y tau = C/UA.
 */
public class ModeloTermico {

    private double capacidadTermica; // C [J/K]
    private double uA;               // UA [W/K]
    private double potenciaEntrada;  // P_in [W] por defecto (opcional)

    // Constructor 3-args (mantener para compat con Main)
    public ModeloTermico(double capacidadTermica, double uA, double potenciaEntradaPorDefecto) {
        if (capacidadTermica <= 0 || uA <= 0) {
            throw new IllegalArgumentException("C y UA deben ser > 0");
        }
        this.capacidadTermica = capacidadTermica;
        this.uA = uA;
        this.potenciaEntrada = potenciaEntradaPorDefecto;
    }

    // Constructor 2-args (para SensorMqttRunner)
    public ModeloTermico(double capacidadTermica, double uA) {
        this(capacidadTermica, uA, 0.0);
    }

    public double calcularTemperaturaUnaHora(double tempInteriorInicial, double tempExterior) {
        return calcularTemperaturaConPotencia(tempInteriorInicial, tempExterior, this.potenciaEntrada, 3600.0);
    }

    /** Permite indicar P_in específico para esa hora */
    public double calcularTemperaturaUnaHoraConPotencia(double tempInteriorInicial, double tempExterior, double potenciaHora) {
        return calcularTemperaturaConPotencia(tempInteriorInicial, tempExterior, potenciaHora, 3600.0);
    }

    private double calcularTemperaturaConPotencia(double T0, double Tout, double Pin, double tSeconds) {
        double tau = capacidadTermica / uA;         // s
        double Tss = Tout + Pin / uA;               // °C
        return Tss + (T0 - Tss) * Math.exp(-tSeconds / tau);
    }

    /** Un paso de dt (segundos) con potencia Pin (W) */
    public double paso(double tempInterior, double tempExterior, double potenciaW, double dtSeconds) {
        double dTdt = (potenciaW - uA * (tempInterior - tempExterior)) / capacidadTermica;
        return tempInterior + dTdt * dtSeconds;
    }

    // Getters/Setters
    public double getCapacidadTermica() { return capacidadTermica; }
    public void setCapacidadTermica(double capacidadTermica) { this.capacidadTermica = capacidadTermica; }
    public double getUA() { return uA; }
    public void setUA(double uA) { this.uA = uA; }
    public double getPotenciaEntrada() { return potenciaEntrada; }
    public void setPotenciaEntrada(double potenciaEntrada) { this.potenciaEntrada = potenciaEntrada; }
}
