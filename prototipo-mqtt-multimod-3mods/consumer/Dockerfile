# ===== build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY api/pom.xml api/pom.xml
COPY simulator/pom.xml simulator/pom.xml
COPY consumer/pom.xml consumer/pom.xml

# cache deps del reactor
RUN mvn -q -U -DskipTests -pl consumer -am dependency:go-offline

# código del consumer
COPY consumer/src consumer/src

# empaquetar
RUN mvn -q -U -DskipTests -pl consumer -am package

# ===== run =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/consumer/target/consumer.jar app.jar

ENV BROKER_HOST=broker-mqtt \
    BROKER_PORT=1883 \
    TOPIC_OUTDOOR=sensors/outdoor/temperature \
    TOPIC_INDOOR=sensors/indoor/temperature \
    SETPOINT=21

ENTRYPOINT ["java","-jar","/app/app.jar"]
