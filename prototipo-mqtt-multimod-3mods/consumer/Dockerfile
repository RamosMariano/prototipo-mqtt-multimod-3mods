# ====== BUILD ======
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copiá POMs para cachear deps
COPY pom.xml .
COPY api/pom.xml api/pom.xml
COPY consumer/pom.xml consumer/pom.xml
COPY simulator/pom.xml simulator/pom.xml
RUN mvn -q -DskipTests -pl consumer -am dependency:go-offline

# Copiá el código
COPY . .

# Empaquetá solo consumer (shade genera consumer.jar)
RUN mvn -q -DskipTests -pl consumer -am package

# ====== RUNTIME ======
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/consumer/target/consumer.jar /app/consumer.jar
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
CMD ["java","-jar","/app/consumer.jar"]
