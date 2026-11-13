# syntax=docker/dockerfile:1.7

# ==============================================================================
# Stage 1: Build con Java 25 (Amazon Corretto) + Maven Wrapper
# ==============================================================================
FROM amazoncorretto:25-alpine AS builder
WORKDIR /build

RUN apk add --no-cache bash curl unzip

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests \
    && cp target/restaurant-pos-erp-*.jar /build/app.jar

# ==============================================================================
# Stage 2: Runtime mínimo Non-Root
# ==============================================================================
FROM amazoncorretto:25-alpine AS runtime
WORKDIR /app

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup \
    && mkdir -p /app/logs /app/uploads/dishes \
    && chown -R 1001:1001 /app

COPY --from=builder --chown=1001:1001 /build/app.jar /app/app.jar

ENV PORT=8080 \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER 1001:1001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health" | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
