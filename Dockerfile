# ---------- Etapa 1: compilación ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Descarga dependencias en una capa propia (se reutiliza si el pom.xml no cambia)
RUN mvn -B -q dependency:go-offline
COPY lombok.config .
COPY src ./src
# Las pruebas y la cobertura ya se ejecutan en el pipeline (job build-test)
RUN mvn -B -q clean package -DskipTests -Djacoco.skip=true

# ---------- Etapa 2: imagen final (solo JRE) ----------
FROM eclipse-temurin:21-jre-alpine

# SHA del commit, entregado por el pipeline con --build-arg GIT_COMMIT=<sha>
ARG GIT_COMMIT=local
LABEL org.opencontainers.image.title="peliculas" \
      org.opencontainers.image.source="https://github.com/Daniloko12/devops_007d_ols" \
      org.opencontainers.image.revision="${GIT_COMMIT}"

# Usuario sin privilegios con UID numérico (Kubernetes puede verificar runAsNonRoot)
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=builder --chown=10001:10001 /app/target/*.jar app.jar
USER 10001

# APP_COMMIT se publica en /actuator/info y en la métrica app_build_version.
# MaxRAMPercentage: la JVM usa como máximo el 75 % del límite de memoria del contenedor.
ENV APP_COMMIT=${GIT_COMMIT} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
