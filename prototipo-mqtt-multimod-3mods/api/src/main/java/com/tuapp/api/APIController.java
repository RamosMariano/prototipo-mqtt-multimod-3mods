package com.tuapp.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tuapp.api.dto.EnergiaDTO; // Importar MongoReading
import com.tuapp.api.dto.HumedadDTO; // Importar RawEntry
import com.tuapp.api.dto.Room;
import com.tuapp.api.dto.Sensor;
import com.tuapp.api.dto.Switch;
import com.tuapp.api.dto.TemperaturaDTO;
import com.tuapp.api.mongo.MongoReading;
import com.tuapp.api.mongo.MongoReading.RawEntry;
import com.tuapp.api.mongo.MongoRepo;

@RestController
@RequestMapping("/api")
public class APIController {
    
    private final MongoRepo readingRepo;

    public APIController(MongoRepo readingRepo) {
        this.readingRepo = readingRepo;
    }

    // QUEDA PARA ARMAR A FUTURO
    // ---- ROOMS ----
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public Room createRoom(@RequestBody Object body) {
        List<Sensor> sensores = Arrays.asList(
            new Sensor("1"),
            new Sensor("2")
        );
        List<Switch> switches = Arrays.asList(
            new Switch("101")
        );
        return new Room("room-test-1", sensores, switches);
    }

    // --------------------------------------------------------------------------
    // ---- SENSORS ----

    /**
     * cURL Test: Consultar última temperatura
     * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/rooms/room1/sensors/1/temperature/latest"
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/latest")
    public TemperaturaDTO getLatestTemperature(
        @PathVariable String roomId,
        @PathVariable String sensorId) {

    // Por ahora no usamos para nada el sensorId, pero lo dejamos en la ruta para la proxima iteracion.
    String topic = "sensors/" + roomId + "/temperature";
    
    Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
    
    if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for sensor " + sensorId + " in room " + roomId);
    }

    // Obtener la última lectura (último elemento de la lista raws)
    MongoReading reading = readingOpt.get();
    RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);
    
    if (latestRaw.getPayload().containsKey("temperature")) {
        Double temp = ((Number) latestRaw.getPayload().get("temperature")).doubleValue();
        
        return new TemperaturaDTO(
            temp, 
            "°C", 
            Instant.ofEpochMilli(latestRaw.getTs())
        );
    } else {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Temperature data not found in latest payload for topic " + topic);
    }
    }

    /**
     * cURL Test: Consultar última humedad
     * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/rooms/room1/sensors/1/humidity/latest"
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/latest")
public HumedadDTO getLatestHumidity(
        @PathVariable String roomId,
        @PathVariable String sensorId) {
            
    String topic = "sensors/" + roomId + "/humidity"; 
    Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
    
    if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for sensor " + sensorId + " in room " + roomId);
    }

    MongoReading reading = readingOpt.get();
    RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);
    
    if (latestRaw.getPayload() != null && latestRaw.getPayload().containsKey("humidity")) {
        Object humValue = latestRaw.getPayload().get("humidity");
        Double hum;
        if (humValue instanceof String string) {
            try {
                hum = Double.valueOf(string);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Humidity value is not a valid number.");
            }
        } else {
             if (humValue == null) {
                 throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Humidity value is null.");
             }
             hum = ((Number) humValue).doubleValue();
        }
        
        return new HumedadDTO(
            hum, 
            "%", 
            Instant.ofEpochMilli(latestRaw.getTs())
        );
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Humidity data not found in latest payload for topic " + topic);
        }
    }

    /**
     * cURL Test: Consultar secuencia de temperatura
     * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/rooms/room1/sensors/1/temperature/sequence?fechaInicio=2025-01-01T00:00:00Z&fechaFin=2025-01-02T00:00:00Z"
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/sequence")
    public List<TemperaturaDTO> getTemperatureSequence(
        @PathVariable String roomId,
        @PathVariable String sensorId,
        @RequestParam String fechaInicio,
        @RequestParam String fechaFin) {

    String topic = "sensors/" + roomId + "/temperature"; 
    Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
    
    if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
        return List.of(); // Devuelve lista vacía si no hay datos
    }

    long startTs;
    long endTs;
    try {
        startTs = Instant.parse(fechaInicio).toEpochMilli();
        endTs = Instant.parse(fechaFin).toEpochMilli();
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Use ISO 8601 (e.g., 2025-09-01T00:00:00Z)");
    }
    
    return readingOpt.get().getRaws().stream()
        .filter(raw -> raw.getTs() >= startTs && raw.getTs() <= endTs)
        // **CORRECCIÓN**: Usar "temperature" en lugar de "temp".
        .filter(raw -> raw.getPayload().containsKey("temperature")) 
        .map(raw -> {
            Double temp = ((Number) raw.getPayload().get("temperature")).doubleValue();
            return new TemperaturaDTO(
                temp, 
                "°C", 
                Instant.ofEpochMilli(raw.getTs())
            );
        })
        .collect(Collectors.toList());
    }

    /**
     * cURL Test: Consultar secuencia de humedad
     * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/rooms/room1/sensors/1/humidity/sequence?fechaInicio=2025-01-01T00:00:00Z&fechaFin=2025-01-02T00:00:00Z"
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/sequence")
public List<HumedadDTO> getHumiditySequence(
        @PathVariable String roomId,
        @PathVariable String sensorId,
        @RequestParam String fechaInicio,
        @RequestParam String fechaFin) {

    String topic = "sensors/" + roomId + "/humidity";
    Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
    
    if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
        return List.of(); // Devuelve lista vacía si no hay datos
    }

    long startTs;
    long endTs;
    try {
        startTs = Instant.parse(fechaInicio).toEpochMilli();
        endTs = Instant.parse(fechaFin).toEpochMilli();
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Use ISO 8601 (e.g., 2025-09-01T00:00:00Z)");
    }
    
    return readingOpt.get().getRaws().stream()
        .filter(raw -> raw.getTs() >= startTs && raw.getTs() <= endTs)
        // **CORRECCIÓN**: Usar "humidity" en lugar de "hum".
        .filter(raw -> raw.getPayload().containsKey("humidity")) 
        .map(raw -> {
            Object humValue = raw.getPayload().get("humidity");
            Double hum;
            try {
                 if (humValue instanceof String string) {
                    hum = Double.valueOf(string);
                } else {
                     if (humValue == null) {
                         throw new IllegalArgumentException("Humidity value is null.");
                     }
                     hum = ((Number) humValue).doubleValue();
                }
            } catch (IllegalArgumentException e) {
                // Manejar error en la conversión dentro del stream, puede ser loguear y usar 0.0
                System.err.println("WARN: Error parsing humidity value in sequence: " + e.getMessage());
                hum = 0.0;
            }

            return new HumedadDTO(
                hum, 
                "%", 
                Instant.ofEpochMilli(raw.getTs())
            );
        })
        .collect(Collectors.toList());
    }

    // --------------------------------------------------------------------------
    // ---- SWITCHES ----

   /**
 * cURL Test: Consultar potencia actual (W)
 * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/rooms/room1/switches/1/energy/currentpowerusage"
 */
@GetMapping("/rooms/{roomId}/switches/{switchId}/energy/currentpowerusage")
public EnergiaDTO getCurrentPowerUsage(
        @PathVariable String roomId,
        @PathVariable String switchId) {
            
    String topic = "switches/" + roomId + "/power"; 
    
    Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
    
    if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for switch " + switchId + " in room " + roomId);
    }

    MongoReading reading = readingOpt.get();
    RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);

    if (latestRaw.getPayload().containsKey("power")) {
        Double power = ((Number) latestRaw.getPayload().get("power")).doubleValue();

        String unit = "W";
        
        return new EnergiaDTO(
            power, 
            unit, 
            Instant.ofEpochMilli(latestRaw.getTs())
        );
    } else {
         return new EnergiaDTO(
            0.0, 
            "W", 
            Instant.ofEpochMilli(latestRaw.getTs())
         );
    }
}
    
    /**
     * cURL Test
     * curl -u "IoTEste:1234" -X GET "http://localhost:8080/api/testing"
     */
    @GetMapping("/testing")
    public String getTest() {
            System.out.println("Testing endpoint hit");
            return "Está vivo!";
    }
}