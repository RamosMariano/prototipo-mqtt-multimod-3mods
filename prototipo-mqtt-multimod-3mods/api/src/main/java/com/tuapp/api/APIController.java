package com.tuapp.api;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Importar los DTOs necesarios
import com.tuapp.api.dto.Room;
import com.tuapp.api.dto.Sensor;
import com.tuapp.api.dto.Switch;
import com.tuapp.api.dto.TemperaturaDTO;
import com.tuapp.api.mongo.MongoRepo;
import com.tuapp.api.dto.HumedadDTO;
import com.tuapp.api.dto.EnergiaDTO;
import com.tuapp.api.mongo.MongoReading; // Importar MongoReading
import com.tuapp.api.mongo.MongoReading.RawEntry; // Importar RawEntry

@RestController
@RequestMapping("/api")
public class APIController {
	
	private final MongoRepo readingRepo;

    public APIController(MongoRepo readingRepo) {
        this.readingRepo = readingRepo;
    }

    // El método createRoom se deja como está, ya que es de 'setup' y no de lectura.
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
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/latest")
    public TemperaturaDTO getLatestTemperature(
            @PathVariable String roomId,
            @PathVariable String sensorId) {
        
        String topic = "sensors/" + roomId + "/" + sensorId;
        
        Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
        
        if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for sensor " + sensorId + " in room " + roomId);
        }

        // Obtener la última lectura (último elemento de la lista raws)
        MongoReading reading = readingOpt.get();
        RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);
        
        // Asumimos que el payload JSON tiene la clave "temp"
        if (latestRaw.getPayload().containsKey("temp")) {
            Double temp = ((Number) latestRaw.getPayload().get("temp")).doubleValue();
            
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
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/latest")
    public HumedadDTO getLatestHumidity(
            @PathVariable String roomId,
            @PathVariable String sensorId) {
            
        String topic = "sensors/" + roomId + "/" + sensorId;
        Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
        
        if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for sensor " + sensorId + " in room " + roomId);
        }

        MongoReading reading = readingOpt.get();
        RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);
        
        // Asumimos que el payload JSON tiene la clave "hum"
        if (latestRaw.getPayload().containsKey("hum")) {
            Double hum = ((Number) latestRaw.getPayload().get("hum")).doubleValue();
            
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
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/temperature/sequence")
    public List<TemperaturaDTO> getTemperatureSequence(
            @PathVariable String roomId,
            @PathVariable String sensorId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
            
        String topic = "sensors/" + roomId + "/" + sensorId;
        Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
        
        if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
            return List.of(); // Devuelve lista vacía si no hay datos
        }

        // Convertir String a Instant y luego a milisegundos para filtrar
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
            .filter(raw -> raw.getPayload().containsKey("temp")) // Solo si contiene temp
            .map(raw -> {
                Double temp = ((Number) raw.getPayload().get("temp")).doubleValue();
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
     */
    @GetMapping("/rooms/{roomId}/sensors/{sensorId}/humidity/sequence")
    public List<HumedadDTO> getHumiditySequence(
            @PathVariable String roomId,
            @PathVariable String sensorId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
            
        String topic = "sensors/" + roomId + "/" + sensorId;
        Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
        
        if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
            return List.of(); // Devuelve lista vacía si no hay datos
        }

        // Convertir String a Instant y luego a milisegundos para filtrar
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
            .filter(raw -> raw.getPayload().containsKey("hum")) // Solo si contiene hum
            .map(raw -> {
                Double hum = ((Number) raw.getPayload().get("hum")).doubleValue();
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
     * cURL Test: Consultar consumo energético
     */
    @GetMapping("/rooms/{roomId}/switches/{switchId}/energy/consumption")
    public EnergiaDTO getEnergyConsumption(
            @PathVariable String roomId,
            @PathVariable String switchId) {
            
        String topic = "switches/" + roomId + "/" + switchId;
        Optional<MongoReading> readingOpt = readingRepo.findByTopic(topic);
        
        if (readingOpt.isEmpty() || readingOpt.get().getRaws().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No data found for switch " + switchId + " in room " + roomId);
        }

        MongoReading reading = readingOpt.get();
        RawEntry latestRaw = reading.getRaws().get(reading.getRaws().size() - 1);
        
        // Asumimos que el payload JSON tiene la clave "energy_kwh"
        if (latestRaw.getPayload().containsKey("energy_kwh")) {
            Double energy = ((Number) latestRaw.getPayload().get("energy_kwh")).doubleValue();
            
            return new EnergiaDTO(
                energy, 
                "kWh", 
                Instant.ofEpochMilli(latestRaw.getTs())
            );
        } else {
            // Si el payload del switch no contiene la clave de energía, devolvemos 0 o lanzamos excepción
             return new EnergiaDTO(
                0.0, 
                "kWh", 
                Instant.ofEpochMilli(latestRaw.getTs())
            );
        }
    }
}