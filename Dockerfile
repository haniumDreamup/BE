# Build stage
FROM gradle:8.14-jdk17 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Create user for running application
RUN groupadd -r bifai && useradd -r -g bifai bifai

# Create directories
RUN mkdir -p /var/log/bifai /var/bifai/files /app/logs && \
    chown -R bifai:bifai /var/log/bifai /var/bifai/files /app/logs

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Switch to non-root user
USER bifai

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", \
  "-XX:-UseContainerSupport", \
  "-Dmanagement.metrics.enabled=false", \
  "-Dspring.jmx.enabled=false", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]