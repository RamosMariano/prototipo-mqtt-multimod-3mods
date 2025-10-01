package com.tuapp.api;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Importar los DTOs necesarios
import com.tuapp.api.dto.Room;
import com.tuapp.api.dto.Sensor;
import com.tuapp.api.dto.Switch;
import com.tuapp.api.dto.TemperaturaDTO;
import com.tuapp.api.dto.HumedadDTO;
import com.tuapp.api.dto.EnergiaDTO;

@RestController
@RequestMapping("/api")
public class APIController {

    // ---- ROOMS ----

    /**
     * cURL Test: Crear una nueva habitación
     curl -X POST "http://localhost:8080/api/rooms" -H "Content-Type: application/json" -d "{\"sensors\": 3}" --user "IoTEste:1234"
     */
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED) // Corresponde al 201 de tu OpenAPI
    public Room createRoom(@RequestBody Object body) {
        // En un controlador real, procesarías 'body' para saber cuántos sensores crear.
        // Aquí retornamos un objeto Room de prueba (Schema Room).
        
        List<Sensor> sensores = Arrays.asList(
            new Sensor("1"),
            new Sensor("2")
        );
        List<Switch> switches = Arrays.asList(
            new Switch("101")
        );
        
        return new Room("room-test-1", sensores, switches);
    }

    // ---- SENSORS ----

    /**
     * cURL Test: Consultar última temperatura
     curl -X GET "http://localhost:8080/api/rooms/room-1/sensors/1/temperature/latest" --user "IoTEste:1234"
     */
    // Schema: LecturaTemperatura
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/latest")
    public TemperaturaDTO getLatestTemperature(
            @PathVariable String roomId,
            @PathVariable String sensorId) {
        
        return new TemperaturaDTO(
            22.4, 
            "°C", 
            Instant.now()
        );
    }

    /**
     * cURL Test: Consultar última humedad
     curl -X GET "http://localhost:8080/api/rooms/room-1/sensors/1/humidity/latest" --user "IoTEste:1234"
     */
    // Schema: LecturaHumedad
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/latest")
    public HumedadDTO getLatestHumidity(
            @PathVariable String roomId,
            @PathVariable String sensorId) {
            
        return new HumedadDTO(
            45.2, 
            "%", 
            Instant.now()
        );
    }

    /**
     * cURL Test: Consultar secuencia de temperatura
     curl -X GET "http://localhost:8080/api/rooms/room-1/sensors/1/temperature/sequence?fechaInicio=2025-09-01T00:00:00Z&fechaFin=2025-09-30T23:59:59Z" --user "IoTEste:1234"
     */
    // Schema: SecuenciaTemperatura (List<LecturaTemperatura>)
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/sequence")
    public List<TemperaturaDTO> getTemperatureSequence(
            @PathVariable String roomId,
            @PathVariable String sensorId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
            
        // Retorna una lista de dos lecturas de prueba
        return Arrays.asList(
            new TemperaturaDTO(23.0, "°C", Instant.now().minusSeconds(3600)),
            new TemperaturaDTO(23.5, "°C", Instant.now())
        );
    }

    /**
     * cURL Test: Consultar secuencia de humedad
     curl -X GET "http://localhost:8080/api/rooms/room-1/sensors/1/humidity/sequence?fechaInicio=2025-09-01T00:00:00Z&fechaFin=2025-09-30T23:59:59Z" --user "IoTEste:1234"
     */
    // Schema: SecuenciaHumedad (List<LecturaHumedad>)
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/sequence")
    public List<HumedadDTO> getHumiditySequence(
            @PathVariable String roomId,
            @PathVariable String sensorId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
            
        // Retorna una lista de dos lecturas de prueba
        return Arrays.asList(
            new HumedadDTO(40.0, "%", Instant.now().minusSeconds(3600)),
            new HumedadDTO(41.5, "%", Instant.now())
        );
    }

    // ---- SWITCHES ----

    /**
     * cURL Test: Consultar consumo energético
     curl -X GET "http://localhost:8080/api/rooms/room-1/switches/101/energy/consumption" --user "IoTEste:1234"
     */
    // Schema: EnergiaShelly
    @GetMapping("/rooms/{roomId}/switches/{switchId}/energy/consumption")
    public EnergiaDTO getEnergyConsumption(
            @PathVariable String roomId,
            @PathVariable String switchId) {
            
        return new EnergiaDTO(
            3.75, 
            "kWh", 
            Instant.now().minusSeconds(86400) // 24 horas atrás
        );
    }
}