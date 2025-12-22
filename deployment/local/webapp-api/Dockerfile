# Backend Dockerfile - FlagForge Spring Boot API
# Multi-stage build for production

# Stage 1: Build the JAR
FROM gradle:8.11-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and properties
COPY gradlew gradle.properties ./
COPY gradle/ gradle/

# Use Docker-specific settings that only includes webapp-api
COPY deployment/local/webapp-api/settings.docker.gradle.kts settings.gradle.kts

# Copy backend build files (maintain backend: hierarchy)
COPY backend/build.gradle.kts backend/
COPY backend/backend-common/build.gradle.kts backend/backend-common/
COPY backend/webapp-api/build.gradle.kts backend/webapp-api/

# Download dependencies (cached layer)
RUN chmod +x gradlew && ./gradlew backend:webapp-api:dependencies --no-daemon || true

# Copy source code
COPY backend/backend-common/src backend/backend-common/src
COPY backend/webapp-api/src backend/webapp-api/src

# Build the JAR
RUN ./gradlew backend:webapp-api:bootJar --no-daemon -x test

# Stage 2: Production runtime
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/backend/webapp-api/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v3/api-docs || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
