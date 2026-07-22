# product-service

Product domain REST service — a Spring Cloud Config **client** in the [`config-management-service`](../README.md) demo (Project 16 of the Spring Boot learning roadmap).

See the [root README](../README.md) for the full cross-service architecture, the complete property-by-property configuration reference, and Docker Compose instructions. This document covers what is specific to `product-service` itself.

## What this module does

Structurally identical to `order-service` (see its [README](../order-service/README.md) for the general config-client pattern), but for the Product domain. Its own packaged `application.yml` is deliberately minimal — `spring.application.name: product-service`, `spring.config.import: optional:configserver:...`, and a local-fallback `server.port: 8082` — everything else is resolved remotely from `config-server`'s `config-repo/product-service.yml` (+ `-docker.yml` overlay).

## Config-driven business rule

`app.product.low-stock-threshold`, bound via the `@RefreshScope`-annotated `ProductProperties` bean (`config/ProductProperties.java`), drives the `lowStock` flag computed in `ProductServiceImpl` for every product returned by the API: any product whose `stockQuantity` is at or below this threshold is flagged `lowStock: true`. Because `ProductProperties` is `@RefreshScope`, raising or lowering the threshold on the Config Server side and calling `POST /actuator/refresh` here immediately changes which products are flagged — no restart, no redeploy.

## Package layout

```
com.medha.configmanagementservice.productservice
├── ProductServiceApplication.java
├── entity/          Product (JPA)
├── dto/             ProductRequest (Bean-Validation annotated), ProductResponse
├── repository/       ProductRepository (Spring Data JPA)
├── service/          ProductService, ProductServiceImpl (business logic + config-driven low-stock rule)
├── controller/        ProductController (CRUD), ConfigInfoController (live-refresh demo endpoint)
├── config/           ProductProperties (@RefreshScope @ConfigurationProperties)
└── exception/         ResourceNotFoundException, ApiError, GlobalExceptionHandler
```

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/products` | `{ name, description, price, stockQuantity }` — 201 on success, 400 on validation failure |
| GET | `/api/products` | List all (each entry flagged `lowStock` against the configured threshold) |
| GET | `/api/products/{id}` | 404 if missing |
| PUT | `/api/products/{id}` | Update |
| DELETE | `/api/products/{id}` | 204 |
| GET | `/api/products/config-info` | Snapshot of the currently-bound `ProductProperties` values — call before/after `/actuator/refresh` to see it change |
| POST | `/actuator/refresh` | Re-pulls config from `config-server`, rebinds `@RefreshScope` beans |
| GET | `/actuator/health` | Health check |

## Running standalone

Requires `config-server` to be reachable at `http://localhost:8888` (or set `CONFIG_SERVER_URL`):

```bash
mvn spring-boot:run
# serving on http://localhost:8082, profile=default
```

## Testing

```bash
mvn test
```

- `ProductServiceImplTest` (Mockito) — `lowStock` flag computed correctly above/below the configured threshold, not-found handling, CRUD delegation.
- `ProductControllerTest` (`@WebMvcTest`) — HTTP status codes and JSON shape for every endpoint.
- `ConfigInfoControllerTest` (`@WebMvcTest`) — the config-info endpoint reflects whatever is currently bound on a mocked `ProductProperties`.
- `ProductServiceApplicationTests` — full context loads with `spring.cloud.config.enabled=false`; Spring Boot auto-configures an embedded H2 datasource since none is declared locally.

## Docker

Two-stage `Dockerfile` (`maven:3.9.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre-alpine` runtime, non-root `spring` user). In `docker-compose.yml`, this service only starts once `config-server` reports healthy, and receives `CONFIG_SERVER_URL=http://config-server:8888` and `SPRING_PROFILES_ACTIVE=docker` so it resolves the `product-service-docker.yml` overlay.
