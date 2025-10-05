# Prototipo MQTT Multimódulos

Proyecto de prototipo IoT basado en **MQTT**, estructurado como **proyecto multimódulo Maven**.  
Incluye simuladores de sensores, un consumidor de mensajes con persistencia en MongoDB y una API con Spring Boot.

---

## 📂 Estructura del proyecto

```
prototipo-mqtt-multimod-3mods/
│
├── api/                # Módulo Spring Boot (exposición de endpoints REST)
├── consumer/           # Módulo Java plano (suscriptor MQTT + persistencia en MongoDB)
├── simulator/          # Módulo Java plano (publicador de mensajes simulados y receptor de órdenes ON/OFF)
├── docker-compose.yml  # Orquestación de broker MQTT + servicios
└── pom.xml             # POM padre (gestiona dependencias y módulos)
```

---

## 🚀 Servicios

- **Broker MQTT** → Eclipse Mosquitto (`broker-mqtt`)  
  Gestiona la comunicación entre publicadores y suscriptores.

- **Consumer** → Suscribe a tópicos MQTT, procesa los mensajes y los **persiste en MongoDB**.  
  Incluye **Mongo Express** como cliente web para visualizar los datos en `http://localhost:8081`.

- **Simulator V2** → Publica datos de sensores simulados (temperatura, humedad, consumo, etc.)  
  Además, **expone endpoints HTTP** para recibir órdenes de encendido y apagado de switches simulados.

- **API** → Servicio Spring Boot que centralizará la lógica de negocio y exposición de datos en futuras versiones.

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

2. El **consumer** se suscribe a los tópicos, procesa los mensajes y los almacena en MongoDB en una colección por `topic`.

3. El **broker Mosquitto** intermedia la comunicación MQTT.

---

## 🧠 Persistencia en MongoDB

El módulo **consumer** guarda los mensajes en MongoDB agrupados por tópico.  
Cada documento incluye los últimos mensajes recibidos hasta un máximo de **110 000 elementos** por tópico.

Mongo Express permite consultar fácilmente la base en:  
👉 **http://localhost:8081**

---

## 🛰️ Endpoints del simulador (ON/OFF switches)

El **simulator V2** ahora incluye un pequeño servidor HTTP embebido para recibir comandos externos.  
Ejemplo de uso con `curl`:

```bash
# Encender calefactor de room1 (puerto 8099)
curl -X POST "http://localhost:8099/rooms/room1/heater?on=true"

# Encender calefactor de room2 (puerto 8100)
curl -X POST "http://localhost:8100/rooms/room2/heater?on=true"
```

El simulador ajusta internamente el estado de los dispositivos y publica los cambios vía MQTT.  
Se corrigieron los valores de **power** y **energy** para reflejar consumos reales simulados.

---

## ⏱️ Configuración del intervalo de publicación del simulador

Variable de entorno `SIM_PUBLISH_INTERVAL_MS` define el **intervalo de publicación** (ms).  
Por defecto: `300000 ms` (5 minutos).

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
      # Intervalo de publicación (default = 300000 ms = 5 min)
      - SIM_PUBLISH_INTERVAL_MS=${SIM_PUBLISH_INTERVAL_MS:-300000}
```

---

## 🛠️ Requisitos

- **Docker** y **Docker Compose**
- **Maven 3.9+**
- **JDK 21+**

---

## 📜 Licencia

Proyecto en desarrollo — uso académico y de pruebas.
