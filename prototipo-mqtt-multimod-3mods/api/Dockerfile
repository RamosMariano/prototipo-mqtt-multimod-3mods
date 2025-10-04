# Build API with Maven from project root
# Usamos esta etapa para compilar todo el código Java
FROM maven:3.9-eclipse-temurin-21 AS build

# Establece el directorio de trabajo donde se copiarán los archivos
WORKDIR /workspace

# Copia todo el contexto del proyecto al contenedor.
COPY . .

# Comando corregido: Ejecuta 'mvn package' sin el flag '-pl api' para compilar
# todos los módulos del proyecto (el reactor completo), asegurando que las clases
# de modelos y repositorios estén disponibles en el classpath al compilar 'api'.
RUN mvn -q -DskipTests package

# Segunda etapa: Crea la imagen final con solo el JRE
FROM eclipse-temurin:21-jre

# Establece el directorio de trabajo para la aplicación
WORKDIR /app

# Copia el JAR compilado de la API (que ahora está garantizado que incluye todas las dependencias)
# El JAR se encuentra en el directorio 'target' del módulo 'api'.
COPY --from=build /workspace/api/target/*.jar /app/app.jar

# Expone el puerto por defecto de Spring Boot
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java","-jar","/app/app.jar"]