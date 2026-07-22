# config-server

Spring Cloud Config Server — the module that serves centralized configuration to `order-service` and `product-service` in the [`config-management-service`](../README.md) demo (Project 16 of the Spring Boot learning roadmap).

See the [root README](../README.md) for the full cross-service architecture, the complete property-by-property configuration reference, and Docker Compose instructions. This document covers what is specific to `config-server` itself.

## What this module does

`ConfigServerApplication` is a plain Spring Boot application with one extra annotation, `@EnableConfigServer`, which turns on the standard Spring Cloud Config Server HTTP contract:

```
GET /{application}/{profile}[/{label}]
GET /{application}-{profile}.yml
GET /{application}-{profile}.properties
```

Any client requesting, say, `GET /order-service/docker` gets back the layered, merged configuration for an application named `order-service` running with the `docker` profile active — resolved from the files in [`config-repo/`](./config-repo).

## Backend: native (filesystem), not Git

This server activates the `native` profile (see `src/main/resources/application.yml`), which tells Spring Cloud Config to read property sources straight off the filesystem/classpath (`config-repo/`) instead of cloning a remote Git URL on each request. This is the simplest backend to learn the Config Server *contract* with — the resolution rules (profile-specific overlay wins over application-wide defaults, which win over the global `application.yml`) are identical to the Git-backed backend; only where the bytes come from differs.

Swapping to a real Git-backed repository later requires **zero client changes** — only this module's `spring.cloud.config.server.*` block would change, e.g.:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/your-org/your-config-repo
          default-label: main
```

## `config-repo/` — the configuration data

| File | Applies to |
|---|---|
| `application.yml` | Every client, every profile (global defaults) |
| `order-service.yml` | `spring.application.name=order-service`, any profile |
| `order-service-docker.yml` | `spring.application.name=order-service`, only `profile=docker` (overlays on top of `order-service.yml`) |
| `product-service.yml` | `spring.application.name=product-service`, any profile |
| `product-service-docker.yml` | `spring.application.name=product-service`, only `profile=docker` |

See the root README's "Configuration Explained" section for a full property-by-property walkthrough of each file.

## Running standalone

```bash
mvn spring-boot:run
# serving on http://localhost:8888
```

Try it:

```bash
curl http://localhost:8888/order-service/default | jq
curl http://localhost:8888/order-service/docker | jq
curl http://localhost:8888/product-service-docker.yml
```

## Testing

```bash
mvn test
```

`ConfigServerApplicationTests` verifies the Spring context — with `@EnableConfigServer` wired in and the `native` profile active against `config-repo/` — starts cleanly. A malformed YAML file or a misconfigured `search-locations` value would fail this test at context-startup time.

## Docker

`Dockerfile` is a two-stage build:

1. `maven:3.9.9-eclipse-temurin-21` compiles `config-server.jar`.
2. `eclipse-temurin:21-jre-alpine` runs it as a non-root `spring` user, with `config-repo/` copied alongside `app.jar` at the same relative path (`./config-repo`) the `native.search-locations` property expects locally.

The image exposes `8888` and includes `wget` so `docker-compose.yml`'s healthcheck (`GET /actuator/health`) can poll it — this is the healthcheck the two client services wait on before they start.
