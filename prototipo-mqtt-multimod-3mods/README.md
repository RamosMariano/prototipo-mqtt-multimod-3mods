# Prototipo MQTT Multimódulos

Proyecto de prototipo IoT basado en **MQTT**, estructurado como **proyecto multimódulo Maven**.  
Incluye simuladores de sensores, un consumidor de mensajes y una API con Spring Boot.

---

## 📂 Estructura del proyecto

```
prototipo-mqtt-multimod-3mods/
│
├── api/                # Módulo Spring Boot (exposición de endpoints REST)
├── consumer/           # Módulo Java plano (suscriptor de tópicos MQTT)
├── simulator/          # Módulo Java plano (publicador de mensajes simulados)
├── docker-compose.yml  # Orquestación de broker MQTT + servicios
└── pom.xml             # POM padre (gestiona dependencias y módulos)
```

---

## 🚀 Servicios

- **Broker MQTT**: Eclipse Mosquitto (contenedor `broker-mqtt`)
- **Consumer**: Suscribe a tópicos MQTT y procesa mensajes
- **Simulator**: Publica datos de sensores simulados (ej. temperatura, humedad)
- **API**: Servicio Spring Boot que en el futuro centralizará la lógica y exposición de datos

---

## ▶️ Cómo levantar el stack

1. **Construir imágenes**
   ```bash
   docker compose build
   ```

2. **Levantar los servicios**
   ```bash
   docker compose up -d
   ```

3. **Ver logs de un servicio (ejemplo: consumer)**
   ```bash
   docker compose logs -f consumer
   ```

4. **Apagar el stack**
   ```bash
   docker compose down
   ```

---

## 📡 Flujo MQTT

1. El **simulator** publica mensajes en tópicos como:
   ```
   sensors/indoor/temperature
   sensors/outdoor/temperature
   ```

   Ejemplo de mensaje:
   ```json
   {
     "unit": "C",
     "type": "temperature",
     "value": 24.9,
     "sensorId": "indoor-ht",
     "ts": 1759095507207
   }
   ```

2. El **consumer** se suscribe a los tópicos, procesa los mensajes y los imprime en logs.

3. El **broker Mosquitto** gestiona la comunicación entre publicadores y suscriptores.

---

## ⏱️ Configuración del intervalo de publicación del simulador


Variable de entorno `SIM_PUBLISH_INTERVAL_MS` para definir el **intervalo de publicación** en milisegundos (por defecto `300000 ms = 5 min`).

Ejemplo en `docker-compose.yml`:

```yaml
  simulator:
    build:
      context: .
      dockerfile: simulator/Dockerfile
    depends_on:
      - broker-mqtt
    environment:
      - BROKER_HOST=broker-mqtt
      - BROKER_PORT=1883
      - TOPIC_OUTDOOR=sensors/outdoor/temperature
      - TOPIC_INDOOR=sensors/indoor/temperature
      - SETPOINT=21
      # Intervalo de publicación en milisegundos (default = 300000 ms = 5 min)
      - SIM_PUBLISH_INTERVAL_MS=${SIM_PUBLISH_INTERVAL_MS:-300000}
```



---

## 🛠️ Requisitos

- Docker y Docker Compose instalados
- Maven 3.9+ y JDK 17+ (para compilar módulos)

---

## 📌 Próximos pasos

- Conectar el **consumer** con la **API Spring Boot**
- Guardar los datos procesados en base de datos
- Exponer endpoints REST en la API para consultar las mediciones
- Agregar tests automáticos con GitHub Actions (CI/CD)

---

## 📜 Licencia

Proyecto en desarrollo — uso académico y de pruebas.
