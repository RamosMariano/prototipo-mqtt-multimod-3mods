# prototipo-mqtt — estructura multimódulos (3 módulos)

Módulos:
- `api` (Spring Boot 3.3) — endpoint `/api/hello`
- `consumer` (Java plano) — fat-jar con maven-assembly-plugin
- `simulator` (Java plano) — fat-jar con maven-assembly-plugin

Se incluyen Dockerfiles multi-stage por módulo y un `docker-compose.yml` para levantar **api + consumer + simulator**.

## Build local
```bash
mvn -q clean package
# Ejecutar planos (si querés probar):
java -jar consumer/target/consumer-jar-with-dependencies.jar
java -jar simulator/target/simulator-jar-with-dependencies.jar
```

## Docker/Compose
```bash
docker compose up -d --build
curl http://localhost:8080/api/hello   # -> API OK
```

### Notas
- Ajustá los `mainClass` en los POM de `consumer` y `simulator` si tu paquete/clase real difiere de:
  - `com.tuapp.consumer.ConsumerMain`
  - `com.tuapp.simulator.app.SimulatorMain`
- Ambos módulos usan Paho (añadí dependencias). Si necesitás más libs, agrégalas en sus `pom.xml`.
- Variables de entorno esperadas: `BROKER_HOST` y `BROKER_PORT` (default `localhost:1883`). Cambiá en `docker-compose.yml` según tu broker real.
