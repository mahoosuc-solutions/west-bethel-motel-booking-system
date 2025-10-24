# Multi-stage Dockerfile for West Bethel Motel Booking System
# Optimized for production with security best practices

#==============================================================================
# Build Stage
#==============================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Build arguments (no secrets here!)
ARG BUILD_VERSION=0.0.1-SNAPSHOT
ARG SKIP_TESTS=true

WORKDIR /workspace

# Copy dependency files first for better layer caching
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -B -DskipTests=${SKIP_TESTS} && \
    mv target/motel-booking-system-${BUILD_VERSION}.jar target/app.jar

#==============================================================================
# Runtime Stage
#==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install security updates and required packages
RUN apk update && \
    apk upgrade && \
    apk add --no-cache \
        curl \
        tini && \
    rm -rf /var/cache/apk/*

# Create non-root user for running the application
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create application directory and log directory
RUN mkdir -p /app /var/log/motel-booking && \
    chown -R appuser:appgroup /app /var/log/motel-booking

WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=appuser:appgroup /workspace/target/app.jar /app/app.jar

# Switch to non-root user
USER appuser

# Environment variables (to be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SERVER_PORT=8080

# Expose application port
EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Use tini as init system to handle signals properly
ENTRYPOINT ["/sbin/tini", "--"]

# Run application with JVM options
CMD ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]

# Metadata labels
LABEL maintainer="West Bethel Motel <noreply@westbethelmotel.com>" \
      description="West Bethel Motel Booking System - Production Image" \
      version="0.0.1-SNAPSHOT" \
      org.opencontainers.image.source="https://github.com/westbethel/motel-booking-system" \
      org.opencontainers.image.documentation="https://github.com/westbethel/motel-booking-system/blob/main/README.md"
