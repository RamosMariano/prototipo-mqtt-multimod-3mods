# Build API with Maven from project root
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl api -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
