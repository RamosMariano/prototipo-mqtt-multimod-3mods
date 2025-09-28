# ===== build =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY consumer/pom.xml consumer/pom.xml
COPY consumer/src consumer/src
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
