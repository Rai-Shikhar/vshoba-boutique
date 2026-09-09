# Multi-stage build for Render (Docker runtime).
# Stage 1: build the Spring Boot fat jar with Maven + JDK 17.
# Stage 2: slim JRE runtime containing only the jar.

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies before copying source (faster rebuilds).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------- runtime ----------------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/boutique-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]