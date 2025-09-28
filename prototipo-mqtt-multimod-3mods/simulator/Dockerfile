# Build simulator module from project root
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl simulator -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/simulator/target/simulator-jar-with-dependencies.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
