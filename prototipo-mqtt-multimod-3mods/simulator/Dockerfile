# ====== BUILD ======
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY api/pom.xml api/pom.xml
COPY consumer/pom.xml consumer/pom.xml
COPY simulator/pom.xml simulator/pom.xml

RUN mvn -q -DskipTests -pl simulator -am dependency:go-offline

COPY . .

RUN mvn -q -DskipTests -pl simulator -am package

# ====== RUNTIME ======
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/simulator/target/simulator.jar /app/simulator.jar

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
CMD ["java","-jar","/app/simulator.jar"]
