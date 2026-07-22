# order-service

Order domain REST service — a Spring Cloud Config **client** in the [`config-management-service`](../README.md) demo (Project 16 of the Spring Boot learning roadmap).

See the [root README](../README.md) for the full cross-service architecture, the complete property-by-property configuration reference, and Docker Compose instructions. This document covers what is specific to `order-service` itself.

## What this module does

A small, standard Spring Boot REST service (entity → repository → service → controller) managing `Order` records against an in-memory H2 database. Structurally it looks like any CRUD microservice — the interesting part is that **none of its runtime configuration lives in its own packaged `application.yml`**. That file only declares:

- `spring.application.name: order-service` — the identity Config Server uses to find `order-service[-<profile>].yml`
- `spring.config.import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}` — where to fetch the rest from
- A local fallback `server.port: 8081`, used only if config-server is unreachable (the `optional:` prefix keeps startup from hard-failing)

Everything else — the datasource, the actuator exposure, the business rules below — is resolved remotely from `config-server`'s `config-repo/order-service.yml` (+ `-docker.yml` overlay) at startup.

## Config-driven business rules

Two properties bound via the `@RefreshScope`-annotated `OrderProperties` bean (`config/OrderProperties.java`) directly drive behavior in `OrderServiceImpl`:

- **`app.order.max-quantity-per-order`** — orders requesting more than this quantity are rejected with HTTP 422 (`OrderLimitExceededException`).
- **`app.order.discount-enabled` / `app.order.discount-percentage`** — when enabled, `totalPrice` is computed net of the configured discount percentage.

Because `OrderProperties` is `@RefreshScope`, both rules can be changed live: edit `config-repo/order-service*.yml` on the Config Server side, then `POST /actuator/refresh` here — no restart, no redeploy.

## Package layout

```
com.medha.configmanagementservice.orderservice
├── OrderServiceApplication.java
├── entity/          Order, OrderStatus (JPA)
├── dto/             OrderRequest (Bean-Validation annotated), OrderResponse
├── repository/       OrderRepository (Spring Data JPA)
├── service/          OrderService, OrderServiceImpl (business logic + config-driven rules)
├── controller/        OrderController (CRUD), ConfigInfoController (live-refresh demo endpoint)
├── config/           OrderProperties (@RefreshScope @ConfigurationProperties)
└── exception/         ResourceNotFoundException, OrderLimitExceededException, ApiError, GlobalExceptionHandler
```

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/orders` | `{ customerName, productName, quantity, unitPrice }` — 201 on success, 422 if quantity exceeds the configured max, 400 on validation failure |
| GET | `/api/orders` | List all |
| GET | `/api/orders/{id}` | 404 if missing |
| PUT | `/api/orders/{id}` | Update |
| DELETE | `/api/orders/{id}` | 204 |
| GET | `/api/orders/config-info` | Snapshot of the currently-bound `OrderProperties` values — call before/after `/actuator/refresh` to see it change |
| POST | `/actuator/refresh` | Re-pulls config from `config-server`, rebinds `@RefreshScope` beans |
| GET | `/actuator/health` | Health check |

## Running standalone

Requires `config-server` to be reachable at `http://localhost:8888` (or set `CONFIG_SERVER_URL`):

```bash
mvn spring-boot:run
# serving on http://localhost:8081, profile=default
```

## Testing

```bash
mvn test
```

- `OrderServiceImplTest` (Mockito) — discount math, max-quantity rejection, CRUD delegation, not-found handling.
- `OrderControllerTest` (`@WebMvcTest`) — HTTP status codes and JSON shape for every endpoint.
- `ConfigInfoControllerTest` (`@WebMvcTest`) — the config-info endpoint reflects whatever is currently bound on a mocked `OrderProperties`.
- `OrderServiceApplicationTests` — full context loads with `spring.cloud.config.enabled=false` (no live Config Server required for this smoke test); Spring Boot auto-configures an embedded H2 datasource since none is declared locally.

## Docker

Two-stage `Dockerfile` (`maven:3.9.9-eclipse-temurin-21` build → `eclipse-temurin:21-jre-alpine` runtime, non-root `spring` user). In `docker-compose.yml`, this service only starts once `config-server` reports healthy, and receives `CONFIG_SERVER_URL=http://config-server:8888` and `SPRING_PROFILES_ACTIVE=docker` so it resolves the `order-service-docker.yml` overlay.
