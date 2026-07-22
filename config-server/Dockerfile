# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /build

# Copy just the POMs first so Maven can resolve/cache dependencies as a
# separate, cacheable layer before any source code is copied in.
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
RUN mvn -q -f config-server/pom.xml dependency:go-offline

COPY config-server/src config-server/src
RUN mvn -q -f config-server/pom.xml package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# wget is used by the docker-compose healthcheck to poll /actuator/health.
RUN apk add --no-cache wget \
    && addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/config-server/target/config-server.jar app.jar

# The "native" config-server backend (see application.yml) resolves
# file:./config-repo relative to the JVM's working directory, so the bundled
# config-repo/ folder is copied alongside the jar at the same relative path
# used locally when running `mvn spring-boot:run` from config-server/.
COPY config-server/config-repo /app/config-repo

RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
