package com.tuapp.simulator.app;

import com.tuapp.simulator.domain.Habitacion;
import com.tuapp.simulator.domain.Calefaccion;
import com.tuapp.simulator.model.SimuladorV2;
import com.tuapp.simulator.mqtt.SensorMqttRunner;

public class SimulatorMain {
  public static void main(String[] args) throws Exception {
    com.tuapp.simulator.mqtt.SensorMqttRunner.main(args);
  }
}
