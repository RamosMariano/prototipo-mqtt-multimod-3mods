# ===== build =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copiamos POM padre y POMs de los módulos para que el reactor exista
COPY pom.xml .
COPY api/pom.xml api/pom.xml
COPY simulator/pom.xml simulator/pom.xml
COPY consumer/pom.xml consumer/pom.xml

# (opcional) preparar caché de dependencias del reactor
RUN mvn -q -DskipTests -pl consumer -am dependency:go-offline

# Ahora sí, copiar el código del módulo consumer
COPY consumer/src consumer/src

# Empaquetar sólo consumer (y lo que necesite)
RUN mvn -q -DskipTests -pl consumer -am package

# ===== run =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/consumer/target/consumer.jar app.jar

ENV BROKER_HOST=broker-mqtt \
    BROKER_PORT=1883 \
    TOPIC_OUTDOOR=sensors/outdoor/temperature \
    TOPIC_INDOOR=sensors/indoor/temperature \
    SETPOINT=21

ENTRYPOINT ["java","-jar","/app/app.jar"]
