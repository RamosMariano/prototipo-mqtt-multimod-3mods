package com.tuapp.simulator.domain;

public class Calefaccion {

    private double potenciaTermica;          // W (térmica entregada)
    private double consumoElectrico;         // W (eléctrica consumida)
    private double capacidadTermicaHabitacion; // J/°C (solo si usás el método ideal)

    public Calefaccion(double potenciaTermica, double consumoElectrico, double capacidadTermicaHabitacion) {
        this.potenciaTermica = potenciaTermica;
        this.consumoElectrico = consumoElectrico;
        this.capacidadTermicaHabitacion = capacidadTermicaHabitacion;
    }

    // --- Bloque "ideal" (lo podés mantener aunque no se use en V2)
    public static class Resultado {
        private final double gradosAportados;   // °C ganados en 1h (ideal)
        private final double energiaConsumida;  // kWh en 1h

        public Resultado(double gradosAportados, double energiaConsumida) {
            this.gradosAportados = gradosAportados;
            this.energiaConsumida = energiaConsumida;
        }
        public double getGradosAportados() { return gradosAportados; }
        public double getEnergiaConsumida() { return energiaConsumida; }
        @Override public String toString() {
            return String.format("En 1h aporta: %.2f °C, Consumo: %.2f kWh", gradosAportados, energiaConsumida);
        }
    }

    public Resultado calcularUnaHora() {
        double energiaAportada = potenciaTermica * 3600;               // J en 1h
        double grados = energiaAportada / capacidadTermicaHabitacion;  // °C (ideal)
        double energiaConsumida = (consumoElectrico * 3600.0) / 3_600_000.0; // kWh
        return new Resultado(grados, energiaConsumida);
    }

    // --- Getters usados por el simulador integrado ---
    public double getPotenciaTermica() { return potenciaTermica; }
    public void setPotenciaTermica(double potenciaTermica) { this.potenciaTermica = potenciaTermica; }

    public double getConsumoElectrico() { return consumoElectrico; }
    public void setConsumoElectrico(double consumoElectrico) { this.consumoElectrico = consumoElectrico; }

    // Alias para compatibilidad con SensorMqttRunner que usaba *W():
    public double getPotenciaTermicaW() { return potenciaTermica; }
    public void setPotenciaTermicaW(double w){ this.potenciaTermica = w; }

    public double getConsumoElectricoW() { return consumoElectrico; }
    public void setConsumoElectricoW(double w){ this.consumoElectrico = w; }
}
