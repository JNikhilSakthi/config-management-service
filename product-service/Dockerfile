# syntax=docker/dockerfile:1

# ---- Build stage -----------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY product-service/pom.xml product-service/pom.xml
RUN mvn -q -f product-service/pom.xml dependency:go-offline

COPY product-service/src product-service/src
RUN mvn -q -f product-service/pom.xml package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/product-service/target/product-service.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
