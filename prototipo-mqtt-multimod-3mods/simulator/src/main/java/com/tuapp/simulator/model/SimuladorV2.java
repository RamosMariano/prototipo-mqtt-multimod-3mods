package com.tuapp.simulator.model;

import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.domain.Habitacion;

import java.util.ArrayList;
import java.util.List;

public class SimuladorV2 {

    public static class PuntoHora {
        public final int hora;
        public final double tempInicial;
        public final boolean calefaccionOn;
        public final double potenciaUsadaW;   // 0 o Pin_calefaccion
        public final double tempFinal;
        public final double consumoKWh;       // 0 o consumo eléctrico de esa hora

        public PuntoHora(int hora, double tempInicial, boolean calefaccionOn,
                         double potenciaUsadaW, double tempFinal, double consumoKWh) {
            this.hora = hora;
            this.tempInicial = tempInicial;
            this.calefaccionOn = calefaccionOn;
            this.potenciaUsadaW = potenciaUsadaW;
            this.tempFinal = tempFinal;
            this.consumoKWh = consumoKWh;
        }

        @Override
        public String toString() {
            return String.format(
                    "h=%d | T0=%.2f°C | %s Pin=%.0f W -> Tfinal=%.2f°C | kWh=%.2f",
                    hora, tempInicial,
                    calefaccionOn ? "ON" : "OFF",
                    potenciaUsadaW, tempFinal, consumoKWh
            );
        }
    }

    public static class Resultado {
        public final List<PuntoHora> puntos;
        public final double consumoTotalKWh;

        public Resultado(List<PuntoHora> puntos, double consumoTotalKWh) {
            this.puntos = puntos;
            this.consumoTotalKWh = consumoTotalKWh;
        }
    }

    /**
     * Simula 'horas' horas:
     * - Temp exterior fija (p.ej. 8 °C).
     * - Temp interior inicial tomada de la Habitacion (y se va actualizando).
     * - Si la T pasiva queda <= (termostato - 3°C), recalcula esa hora con P_in = potencia térmica de Calefaccion.
     * - Suma consumo eléctrico de Calefaccion solo en las horas ON.
     */
    public static Resultado simularHoras(
            int horas,
            Habitacion habitacion,
            ModeloTermico modelo,
            Calefaccion calefaccion,
            double termostato,
            double tempExteriorFija
    ) {
        if (horas <= 0) throw new IllegalArgumentException("Horas debe ser > 0");

        List<PuntoHora> timeline = new ArrayList<>();
        double consumoAcumKWh = 0.0;

        for (int h = 1; h <= horas; h++) {
            double T0 = habitacion.getTemperaturaInterior();

            // 1) Predicción pasiva con P_in = 0
            double Tpasiva = modelo.calcularTemperaturaUnaHoraConPotencia(T0, tempExteriorFija, 0.0);

            boolean encender = (Tpasiva <= termostato - 3.0);
            double PinHora = encender ? calefaccion.getPotenciaTermica() : 0.0;

            // 2) Cálculo "integrado": si hay calefacción, la hora se calcula con P_in = PinHora
            double Tfinal = modelo.calcularTemperaturaUnaHoraConPotencia(T0, tempExteriorFija, PinHora);

            // 3) Consumo (solo si ON). Usamos potencia eléctrica real de Calefaccion durante 1 h.
            double kWh = 0.0;
            if (encender) {
                // energía = P_el [W] * 3600 s -> kWh
                kWh = (calefaccion.getConsumoElectrico() * 3600.0) / 3_600_000.0;
            }

            // 4) Actualizar habitación
            habitacion.setTemperaturaInterior(Tfinal);
            consumoAcumKWh += kWh;

            timeline.add(new PuntoHora(h, T0, encender, PinHora, Tfinal, kWh));
        }

        return new Resultado(timeline, consumoAcumKWh);
    }
}

