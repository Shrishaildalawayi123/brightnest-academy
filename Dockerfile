# =============================================
# Dockerfile - BrightNest Academy
# Multi-stage build for production deployment
# Hardened: non-root, slim JRE, healthcheck
# =============================================

# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run (slim JRE)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Security: minimal packages + create non-root user
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home --home-dir /home/academy academy

# Copy artifact before switching user
COPY --from=build --chown=academy:academy /app/target/*.jar app.jar

# Drop to non-root user
USER academy

# Health check — hit the public /health endpoint every 30s
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/health | grep -q '"status"' || exit 1

# Only expose the app port
EXPOSE 8080

# JVM tuning for containers + security hardening
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dcom.sun.management.jmxremote=false \
    -Dspring.jmx.enabled=false"

# Labels for traceability
LABEL maintainer="BrightNest Academy" \
      version="1.0.0" \
      description="BrightNest Academy - Production Build"

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
