# =============================================
# Dockerfile - BrightNest Academy
# Multi-stage build for production deployment
# =============================================

# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S academy && adduser -S academy -G academy
USER academy

COPY --from=build /app/target/*.jar app.jar

# Render sets PORT env var automatically
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
