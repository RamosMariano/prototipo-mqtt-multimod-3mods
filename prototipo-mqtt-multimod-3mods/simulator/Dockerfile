# Etapa de build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copiá solo el pom primero para aprovechar cache
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
# Ahora copiá el código
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa runtime (JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copiá el JAR empacado por assembly/shade
# Si usás assembly:
COPY target/simulator-jar-with-dependencies.jar /app/simulator.jar
# Si usás shade y el JAR se llama simulator.jar, ajustá esta línea.

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
CMD ["java","-jar","/app/simulator.jar"]
