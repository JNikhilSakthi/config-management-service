# Config Management Service — Centralized Configuration Demo

Externalize environment-specific configuration for a fleet of microservices into one Spring Cloud Config Server, so every service can be built once and reconfigured per environment without a rebuild — and refreshed live without a restart.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-brightgreen)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![Docker](https://img.shields.io/badge/Container-Docker-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## Learning Track & Real-World Identity

- **Learning Track**: `springboot-config-server-demo` (Project 16 of 17 in the Spring Boot learning roadmap)
- **Real-World Service Name**: `config-management-service`
- **Technology Focus**: Spring Cloud Config Server
- **Additional Components**: Config Server + native (filesystem) config repository
- **Example Project**: Centralized Configuration Demo

This repository deliberately isolates **one** concept — Spring Cloud Config — end to end. It does **not** pull in Eureka, an API Gateway, OpenFeign, or Resilience4j; those are separate projects elsewhere in the roadmap. The two client services (`order-service`, `product-service`) exist only as believable, minimal consumers of centralized configuration — their CRUD logic is intentionally simple so that the Config Server behavior stays the star of the show.

---

## 3. Project Overview

### The problem this solves

In a system with more than a handful of microservices, every service traditionally ships its own `application.yml` baked into its JAR/image. That creates real, expensive pain:

- **Config drift**: the same logical setting (e.g. a feature flag, a downstream URL, a connection-pool size) is duplicated across N repos and inevitably diverges.
- **Rebuild-to-reconfigure**: changing a timeout or a log level means editing code, rebuilding, and re-deploying an artifact — even though nothing about the *application logic* changed.
- **No single source of truth for "what is prod actually running with"** — an incident responder has to SSH into a box or read image layers to find out.
- **No safe way to change config live**: without a mechanism like Config Server + `@RefreshScope`, the only way to pick up a new value is to restart the process, which means downtime (or careful rolling restarts) for what should be a zero-risk change.

### Why centralized configuration

Spring Cloud Config Server solves this by moving configuration **out of the deployable artifact** and into a Config Server that all services ask, at startup, "given my name and my active profile, what are my properties?" The server answers from one place — here, a folder of YAML files (`config-repo/`), but in production typically a Git repository, so config changes get the same review/audit/rollback story as code changes.

Combined with `@RefreshScope` and the `/actuator/refresh` endpoint, a subset of that configuration can even be pushed into an **already-running** JVM: no restart, no downtime, full audit trail (a git commit) of exactly what changed and when.

### Where this is used in real companies

This exact pattern — Config Server (or an equivalent: Consul, etcd, AWS AppConfig/Parameter Store, HashiCorp Vault for secrets) — backs configuration for fleets of Spring Boot microservices at organizations running Spring Cloud Netflix/Alibaba-style architectures (finance, e-commerce, telecom backends). It is also the direct conceptual ancestor of Kubernetes `ConfigMap`/`Secret` + a GitOps controller (Argo CD, Flux) reconciling desired config from Git — see the Interview Preparation section for a detailed comparison.

---

## 4. Architecture

### High-Level Design (HLD)

```
                          ┌────────────────────────────┐
                          │      config-repo/ (Git-     │
                          │   shaped folder of YAML)     │
                          │  application.yml             │
                          │  order-service.yml           │
                          │  order-service-docker.yml     │
                          │  product-service.yml          │
                          │  product-service-docker.yml    │
                          └──────────────┬───────────────┘
                                         │ native backend reads from disk
                                         ▼
                          ┌────────────────────────────┐
                          │       config-server          │
                          │   @EnableConfigServer         │
                          │   port 8888                   │
                          └───────┬───────────────┬───────┘
                  1. GET /order-service/docker     │ 1. GET /product-service/docker
                  at startup (spring.config.import) │ at startup
                                  │                 │
                                  ▼                 ▼
                     ┌───────────────────┐  ┌───────────────────┐
                     │   order-service     │  │   product-service   │
                     │   port 8081         │  │   port 8082          │
                     │  spring-cloud-      │  │  spring-cloud-       │
                     │  starter-config     │  │  starter-config      │
                     │  @RefreshScope bean  │  │  @RefreshScope bean   │
                     └─────────┬──────────┘  └─────────┬───────────┘
                                │                       │
                     2. POST /actuator/refresh          │ 2. POST /actuator/refresh
                     (operator, after a config-repo      │ (operator)
                      change) — re-pulls from            │
                      config-server, rebinds beans        │
```

### Low-Level Design (LLD) — one client's startup + refresh sequence

```
order-service JVM starting
   │
   ├─ spring.config.import = optional:configserver:http://config-server:8888
   │       │
   │       ▼
   │   ConfigServerConfigDataLoader issues:
   │       GET http://config-server:8888/order-service/docker
   │       │
   │       ▼
   │   config-server (native backend) resolves, IN ORDER OF PRECEDENCE:
   │       1. order-service-docker.yml   (profile-specific, wins on conflict)
   │       2. order-service.yml           (application-specific defaults)
   │       3. application.yml             (global defaults for ALL clients)
   │       │
   │       ▼
   │   Merged properties returned as one JSON PropertySources payload
   │       │
   │       ▼
   │   Spring Boot merges them into the Environment BEFORE any
   │   @Configuration class is processed
   │       │
   │       ▼
   │   OrderProperties (@RefreshScope @ConfigurationProperties) binds
   │       app.order.welcome-message / max-quantity-per-order / discount-*
   │
   ▼
order-service is UP, serving /api/orders/* using these values

-- time passes, an operator edits config-repo/order-service-docker.yml --

operator: POST /actuator/refresh  ─────────▶  order-service
   │
   ▼
Spring Cloud Context's RefreshScope destroys the cached OrderProperties bean
   │
   ▼
Next access to OrderProperties triggers lazy re-creation:
   re-fetch GET /order-service/docker from config-server (which re-reads
   the YAML files from disk on every request in native mode)
   │
   ▼
OrderProperties rebinds with the NEW values — no JVM restart occurred
```

### Folder structure

```
springboot-config-server-demo/
├── pom.xml                          # parent aggregator (packaging=pom), 3 modules
├── docker-compose.yml                # config-server -> order-service/product-service
├── README.md                         # this file
├── config-server/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── README.md
│   ├── config-repo/                  # the "native" backend's property source
│   │   ├── application.yml
│   │   ├── order-service.yml
│   │   ├── order-service-docker.yml
│   │   ├── product-service.yml
│   │   └── product-service-docker.yml
│   └── src/
│       ├── main/java/.../configserver/ConfigServerApplication.java
│       ├── main/resources/application.yml
│       └── test/java/.../ConfigServerApplicationTests.java
├── order-service/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── README.md
│   └── src/
│       ├── main/java/.../orderservice/
│       │   ├── OrderServiceApplication.java
│       │   ├── entity/{Order,OrderStatus}.java
│       │   ├── dto/{OrderRequest,OrderResponse}.java
│       │   ├── repository/OrderRepository.java
│       │   ├── service/{OrderService,OrderServiceImpl}.java
│       │   ├── controller/{OrderController,ConfigInfoController}.java
│       │   ├── config/OrderProperties.java
│       │   └── exception/{ResourceNotFoundException,OrderLimitExceededException,ApiError,GlobalExceptionHandler}.java
│       ├── main/resources/application.yml
│       └── test/java/.../ (unit + @WebMvcTest + smoke test)
└── product-service/
    ├── pom.xml
    ├── Dockerfile
    ├── README.md
    └── src/  (same shape as order-service, for the Product domain)
```

### Request flow: how config is fetched at startup and on refresh

1. **Startup**: `order-service`/`product-service` declare `spring.config.import: optional:configserver:${CONFIG_SERVER_URL}` in their own `application.yml`. Spring Boot's config-data import mechanism runs this BEFORE any bean is created, so the fetched properties are indistinguishable, to the rest of the application, from properties that had been written locally.
2. **Config Server resolution**: `config-server` (native profile) reads `config-repo/{application}-{profile}.yml`, then `config-repo/{application}.yml`, then `config-repo/application.yml`, and returns them layered (most specific wins) as one environment payload.
3. **Refresh**: an operator (or a CI/CD pipeline, or a Config Server webhook in a Git-backed setup) edits a file in `config-repo/`. The already-running client is still serving the OLD values until it is told to refresh.
4. **`POST /actuator/refresh`**: this Spring Cloud Context endpoint publishes a `RefreshScopeRefreshedEvent`, evicting every bean in the `refresh` scope (here, `OrderProperties`/`ProductProperties`). The next read re-triggers the whole "ask config-server" flow from step 2, and the bean rebinds with the fresh values — no restart, no downtime.

---

## 5. Tech Stack

| Component | Technology | Version | Why |
|---|---|---|---|
| Language | Java | 21 | LTS, required baseline for the roadmap |
| Framework | Spring Boot | 3.3.4 | Base application framework for all 3 modules |
| Config platform | Spring Cloud Config | 2023.0.3 (`spring-cloud-dependencies` BOM) | Server + client starters |
| Build tool | Maven (multi-module) | 3.9.x | Parent POM + 3 modules, `dependency-management` import of the Spring Cloud BOM |
| Web layer | Spring MVC (`spring-boot-starter-web`) | — | REST controllers on order-service/product-service |
| Persistence | Spring Data JPA + H2 (in-memory) | — | Simple, dependency-free CRUD stores so the demo needs no external DB — the teaching focus stays on Config Server, not on database ops |
| Validation | `spring-boot-starter-validation` (Bean Validation / Jakarta) | — | `@NotBlank`, `@Min`, `@DecimalMin` on request DTOs |
| Observability | `spring-boot-starter-actuator` | — | `/actuator/health`, `/actuator/refresh`, `/actuator/env`, `/actuator/info` |
| Testing | JUnit 5 + Mockito + Spring `MockMvc` | — | Service-layer unit tests, controller slice tests, context-load smoke tests |
| Containerization | Docker (multi-stage builds), Docker Compose | — | `maven:3.9.9-eclipse-temurin-21` build stage → `eclipse-temurin:21-jre-alpine` runtime stage |

---

## 6. Configuration Explained

There are **two layers** of configuration in this repository, and understanding the difference is the entire point of the project:

- Each client's **own** `src/main/resources/application.yml` — small, static, ships inside the JAR. It only knows the client's name and how to find the Config Server.
- The **remote** files in `config-server/config-repo/` — this is where the actual environment-specific behavior lives, and it can change without rebuilding anything.

### `config-server/src/main/resources/application.yml`

```yaml
server:
  port: 8888
spring:
  application:
    name: config-server
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: file:./config-repo,classpath:/config-repo
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

| Property | Meaning |
|---|---|
| `server.port: 8888` | Conventional Config Server port. |
| `spring.application.name: config-server` | This server's own identity (used in its own actuator `/info`, not related to what it serves). |
| `spring.profiles.active: native` | Activates the **native** environment repository backend — reads config from the local filesystem/classpath instead of cloning a remote Git URL. Simplest backend to learn the Config Server contract with. |
| `spring.cloud.config.server.native.search-locations` | Comma-separated Spring resource locations to scan for `{application}[-{profile}].yml` files. `file:./config-repo` covers `mvn spring-boot:run` (relative to the module dir) and the Docker image (where `config-repo/` is copied next to the jar); `classpath:/config-repo` is a fallback if it's ever packaged onto the classpath instead. |
| `management.endpoints.web.exposure.include: health,info` | Config Server itself only needs health/info exposed — it is not a config *client*, so it has no `/actuator/refresh` of its own. |

### `config-server/config-repo/application.yml` (global defaults for ALL clients)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh,env
  endpoint:
    health:
      show-details: always
    env:
      show-values: always
info:
  config-source: "Spring Cloud Config Server (native filesystem backend)"
  config-label: "main"
```

| Property | Meaning |
|---|---|
| `management.endpoints.web.exposure.include: health,info,refresh,env` | Exposed on every **client** (order-service, product-service) that inherits this global file. `refresh` is the whole point of this project — it's what lets an operator push new values into a running JVM. `env` is exposed so you can inspect exactly which `PropertySource` a given key came from (great for debugging precedence). |
| `info.config-source` / `info.config-label` | Purely illustrative "where did my config come from" banner, surfaced by `ConfigInfoController` in each client and by the standard `/actuator/info` endpoint. |

### `config-server/config-repo/order-service.yml` (base config for order-service, all profiles)

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
  h2:
    console:
      enabled: true
      path: /h2-console
app:
  order:
    welcome-message: "Order Service is running with configuration fetched from Config Server (profile=default, label=main)."
    max-quantity-per-order: 10
    discount-enabled: false
    discount-percentage: 0
product-service:
  base-url: http://localhost:8082
```

| Property | Meaning |
|---|---|
| `server.port: 8081` | order-service's HTTP port — note this lives centrally, not in order-service's own `application.yml` (which only has a fallback for offline/no-config-server startup). |
| `spring.datasource.*` | H2 in-memory database connection details; kept simple deliberately (no external DB) so the demo needs nothing beyond the 3 Java processes. |
| `spring.jpa.hibernate.ddl-auto: update` | Auto-creates/updates the schema from the `Order` entity — fine for a demo, never for real production data. |
| `app.order.welcome-message` | A human-readable banner proving which profile/label's config a running instance actually has — surfaced via `GET /api/orders/config-info`. |
| `app.order.max-quantity-per-order` | A **business rule**, not cosmetics: `OrderServiceImpl` rejects orders whose quantity exceeds this value. Demonstrates that Config Server can drive live behavior, not just log lines. |
| `app.order.discount-enabled` / `discount-percentage` | Feature flag + parameter controlling whether/how much discount is applied when computing `totalPrice`. |
| `product-service.base-url` | Where order-service would reach product-service (local/default profile: localhost). |

### `config-server/config-repo/order-service-docker.yml` (overlay, only for `profile=docker`)

```yaml
app:
  order:
    welcome-message: "Order Service is running with configuration fetched from Config Server (profile=docker, label=main)."
    discount-enabled: true
    discount-percentage: 5
product-service:
  base-url: http://product-service:8082
logging:
  level:
    com.medha.configmanagementservice.orderservice: DEBUG
```

| Property | Meaning |
|---|---|
| `app.order.welcome-message` | Overridden banner text — proves, at a glance, which overlay is active. |
| `app.order.discount-enabled` / `discount-percentage` | In the `docker` profile, a 5% discount is turned ON — showing the SAME code path behaving differently per environment purely through config. |
| `product-service.base-url: http://product-service:8082` | Inside the docker-compose network, services resolve each other by **service name**, not `localhost`. |
| `logging.level...: DEBUG` | Docker environment gets more verbose logging by default — a common real-world pattern (quiet in prod-like local runs, loud in the environment engineers actually poke at). |

### `config-server/config-repo/product-service.yml` / `product-service-docker.yml`

Same shape as order-service's pair, for the Product domain:

| Property | Meaning |
|---|---|
| `server.port: 8082` | product-service's HTTP port. |
| `spring.datasource.*` | H2 in-memory `productdb`. |
| `app.product.welcome-message` | Same banner pattern as order-service. |
| `app.product.low-stock-threshold` | Business rule: any product whose `stockQuantity` is at or below this value is flagged `lowStock: true` in API responses. Default profile: `5`; docker profile raises it to `10`, purely to make the override visible. |
| `app.product.price-currency` | Cosmetic metadata (`"INR"`) surfaced via the config-info endpoint. |

### `order-service/src/main/resources/application.yml` (the client's own, minimal file)

```yaml
spring:
  application:
    name: order-service
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
  cloud:
    config:
      name: order-service
      label: main
      fail-fast: true
      retry:
        max-attempts: 5
        initial-interval: 1000
        multiplier: 1.5
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:default}
server:
  port: 8081
```

| Property | Meaning |
|---|---|
| `spring.application.name: order-service` | The name Config Server uses to find `order-service[-<profile>].yml`. Also this client's own service identity. |
| `spring.config.import` | Spring Boot 3.x / Spring Cloud 2022+ style config-client wiring — **no `bootstrap.yml` needed**. The `optional:` prefix means order-service still starts (using only this local fallback config) even if config-server is briefly unreachable, rather than crashing at boot — friendlier while learning/debugging. `${CONFIG_SERVER_URL:http://localhost:8888}` defaults to localhost for a bare `mvn spring-boot:run`, and is overridden to `http://config-server:8888` by docker-compose. |
| `spring.cloud.config.name: order-service` | Explicit override of which `{application}` name to request from the server (defaults to `spring.application.name` anyway; spelled out here for clarity). |
| `spring.cloud.config.label: main` | Maps to a Git branch in a git-backed repo; ignored by the native backend, but wiring it up now means switching backends later needs zero client changes. |
| `spring.cloud.config.fail-fast: true` | Retry aggressively rather than silently falling back — makes connectivity problems loud during learning instead of confusingly silent. |
| `spring.cloud.config.retry.*` | Up to 5 attempts, exponential-ish backoff (1s, 1.5s, 2.25s, ...) while `config-server` is still starting up (relevant even with the docker-compose healthcheck gate). |
| `spring.profiles.active` | Selects which `order-service-<profile>.yml` overlay applies; `default` locally, `docker` inside docker-compose. |
| `server.port: 8081` | Local fallback only — normally this value is served remotely from `config-repo/order-service.yml`. |

`product-service/src/main/resources/application.yml` is identical in shape (see its own README) with `spring.application.name: product-service` and `server.port: 8082`.

---

## 7. Project Structure Explained

| Path | Why it exists |
|---|---|
| `pom.xml` (root) | Parent aggregator POM (`packaging=pom`); centralizes the Spring Boot parent version, the `spring-cloud-dependencies` BOM import, and lists the 3 modules so a single `mvn package` at the root builds everything in the right order. |
| `docker-compose.yml` | Orchestrates all 3 services with the correct startup order: `config-server` first (gated on its own healthcheck), then `order-service`/`product-service` (gated on `config-server` being healthy). |
| `config-server/` | The Spring Cloud Config Server module — see its own README for a property-by-property breakdown. |
| `config-server/config-repo/` | The actual configuration **data** — 5 YAML files playing the role a Git repository would play in production. Bundled inside the module (and copied into its Docker image) so the whole demo is self-contained with no external Git server dependency. |
| `order-service/`, `product-service/` | Two structurally-identical config-client microservices, each with its own `entity/repository/service/controller/exception/config` package layout, its own tests, and its own Dockerfile. |
| `*/README.md` | Per-module documentation focused on that module's responsibilities. |
| `LICENSE` | MIT license text for the published repository. |

---

## 8. Getting Started

### Prerequisites

- Docker + Docker Compose (Docker Desktop or equivalent)
- (Optional, for local non-Docker development) JDK 21 and Maven 3.9+

### Run the whole stack with Docker Compose — correct startup order

From the repository root:

```bash
# Build all 3 images (config-server, order-service, product-service)
docker compose build

# Start config-server FIRST; order-service/product-service only start once
# config-server reports healthy (see docker-compose.yml depends_on/healthcheck)
docker compose up -d

# Watch it come up in order
docker compose ps
#  NAME              STATUS
#  config-server     Up (healthy)
#  order-service     Up (healthy)
#  product-service   Up (healthy)

# Tail logs if you want to watch the config-fetch-at-startup sequence
docker compose logs -f config-server order-service product-service
```

Shut everything down:

```bash
docker compose down
```

### Run locally without Docker (for active development)

```bash
# Terminal 1 — Config Server
cd config-server
mvn spring-boot:run
# now serving http://localhost:8888

# Terminal 2 — order-service (uses the "default" profile overlay)
cd order-service
mvn spring-boot:run
# now serving http://localhost:8081

# Terminal 3 — product-service
cd product-service
mvn spring-boot:run
# now serving http://localhost:8082
```

Config Server must be started **before** (or at least, before the clients finish their startup retry window — see `spring.cloud.config.retry`) the two client services, exactly as enforced by the Docker Compose healthcheck dependency.

---

## 9. API Documentation

### config-server (port 8888)

| Method | Path | Description |
|---|---|---|
| GET | `/{application}/{profile}` | Raw Config Server contract, e.g. `GET /order-service/docker` returns the merged, layered property sources for that application+profile. |
| GET | `/{application}-{profile}.yml` | Same data, rendered as a flat YAML document. |
| GET | `/actuator/health` | Liveness/readiness — what the docker-compose healthcheck polls. |

### order-service (port 8081)

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Create an order. Body: `{ "customerName", "productName", "quantity", "unitPrice" }`. Rejects (422) if `quantity` exceeds the config-driven `app.order.max-quantity-per-order`. |
| GET | `/api/orders` | List all orders. |
| GET | `/api/orders/{id}` | Get one order (404 if missing). |
| PUT | `/api/orders/{id}` | Update an order. |
| DELETE | `/api/orders/{id}` | Delete an order. |
| GET | `/api/orders/config-info` | Returns the CURRENT values bound onto the `@RefreshScope` `OrderProperties` bean — call before/after `POST /actuator/refresh` to see it change live. |
| POST | `/actuator/refresh` | Re-pulls configuration from config-server and rebinds every `@RefreshScope` bean. |
| GET | `/actuator/health` | Health check. |

Example:

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Nikhil","productName":"Keyboard","quantity":2,"unitPrice":50.00}'
```

### product-service (port 8082)

| Method | Path | Description |
|---|---|---|
| POST | `/api/products` | Create a product. Body: `{ "name", "description", "price", "stockQuantity" }`. |
| GET | `/api/products` | List all products (each flagged `lowStock: true/false` against the config-driven threshold). |
| GET | `/api/products/{id}` | Get one product (404 if missing). |
| PUT | `/api/products/{id}` | Update a product. |
| DELETE | `/api/products/{id}` | Delete a product. |
| GET | `/api/products/config-info` | Current values bound onto the `@RefreshScope` `ProductProperties` bean. |
| POST | `/actuator/refresh` | Re-pulls configuration, rebinds `@RefreshScope` beans. |
| GET | `/actuator/health` | Health check. |

### Demonstrating live refresh end-to-end

```bash
# 1. Observe the current value
curl http://localhost:8081/api/orders/config-info

# 2. Edit config-server/config-repo/order-service-docker.yml, e.g. change
#    discount-percentage: 5  ->  discount-percentage: 15
#    (in a real deployment this is a git commit + push to the config repo)

# 3. Copy the change into the running config-server (or restart it, or use a
#    real Git-backed repo where config-server re-clones on each request):
docker cp config-server/config-repo/order-service-docker.yml config-server:/app/config-repo/order-service-docker.yml

# 4. Tell order-service to refresh
curl -X POST http://localhost:8081/actuator/refresh

# 5. Observe the NEW value — no restart occurred
curl http://localhost:8081/api/orders/config-info
```

---

## 10. Testing

Each service module ships JUnit 5 + Mockito unit tests and Spring `@WebMvcTest` slice tests; `config-server` ships a context-load smoke test. All were verified green on JDK 21.

Run every module's tests from the repository root:

```bash
mvn test
```

Run a single module's tests:

```bash
cd order-service && mvn test
cd product-service && mvn test
cd config-server && mvn test
```

What's covered:

- **`config-server`**: `ConfigServerApplicationTests` — the Spring context (with `@EnableConfigServer` and the `native` profile against `config-repo/`) starts cleanly; a malformed YAML or misconfigured search path would fail this test.
- **`order-service`**:
  - `OrderServiceImplTest` (Mockito) — total-price computation with/without the configured discount, rejection when quantity exceeds the configured max, not-found handling, full CRUD delegation.
  - `OrderControllerTest` (`@WebMvcTest`) — HTTP status codes, JSON shape, validation-triggered 400s, not-found-triggered 404s.
  - `ConfigInfoControllerTest` (`@WebMvcTest`) — the config-info endpoint faithfully reflects whatever is currently bound on `OrderProperties`.
  - `OrderServiceApplicationTests` — full context loads with Config Server import disabled (`spring.cloud.config.enabled=false`), auto-configuring an embedded H2 datasource.
- **`product-service`**: the same four-test shape, for the Product domain (`ProductServiceImplTest`, `ProductControllerTest`, `ConfigInfoControllerTest`, `ProductServiceApplicationTests`), including a dedicated assertion that `lowStock` flips based on the configured threshold.

---

## 11. Docker

`docker-compose.yml` defines 3 services on one bridge network (`config-net`):

1. **`config-server`** — built from `config-server/Dockerfile` (multi-stage: `maven:3.9.9-eclipse-temurin-21` compiles the jar; `eclipse-temurin:21-jre-alpine` runs it). Its `config-repo/` folder is copied into the image alongside the jar so the `native` backend's `file:./config-repo` search-location resolves identically to a local `mvn spring-boot:run`. Exposes a `wget`-based healthcheck against `/actuator/health`.
2. **`order-service`** / **`product-service`** — each `depends_on: config-server: condition: service_healthy`, so Compose will not even start their containers until config-server's healthcheck passes. Each receives `CONFIG_SERVER_URL=http://config-server:8888` (service-name DNS resolution inside the compose network) and `SPRING_PROFILES_ACTIVE=docker` (selecting the `-docker.yml` overlay from `config-repo/`). Each has its own healthcheck too, so `docker compose ps` gives an at-a-glance readiness view of the whole stack.

Startup order enforced end to end:

```
config-server (build) → config-server (healthy)
        │
        ├──▶ order-service (build) → order-service (healthy)
        └──▶ product-service (build) → product-service (healthy)
```

Rebuild after a code change:

```bash
docker compose up -d --build
```

---

## 12. Interview Preparation

**Q: What problem does Spring Cloud Config actually solve, in one sentence?**
It moves environment-specific configuration out of each service's deployable artifact and into one addressable service, so config can change (and be reviewed/audited/rolled back) independently of code.

**Q: How does a Spring Boot 3.x app connect to Config Server without `bootstrap.yml`?**
Since Spring Cloud 2020.0 (and standard in Boot 3.x / Cloud 2022+), you declare `spring.config.import: configserver:http://host:port` directly in `application.yml`. Spring Boot's config-data import mechanism resolves it during the "config data" phase, before the main `Environment` is finalized — the old `bootstrap.yml`/`bootstrap-context` mechanism is no longer required (and is off by default unless `spring-cloud-starter-bootstrap` is added).

**Q: What does `@RefreshScope` actually do?**
It wraps the bean in a scope whose instances are cached, but destroyable on demand. `POST /actuator/refresh` fires a `RefreshScopeRefreshedEvent`; every bean in `refresh` scope is evicted from the cache. The NEXT method call on that bean transparently triggers lazy re-creation — which re-runs its `@ConfigurationProperties` binding against a freshly re-fetched `Environment`. Regular singleton beans are not affected by refresh at all; only fields **read at call-time** from a `@RefreshScope` bean (or via `Environment`/`@Value` on a `@RefreshScope` bean) see the new value.

**Q: What are the limitations of `/actuator/refresh`?**
- It only affects `@RefreshScope` beans and a handful of framework-level properties (log levels, some `@ConfigurationProperties`). It does **not** re-run `@PostConstruct`, does not rebuild connection pools sized at startup (e.g. `spring.datasource.hikari.maximum-pool-size` changes are NOT picked up), and does not change `server.port` or anything read only once during `ApplicationContext` refresh.
- It must be triggered per-instance. With N replicas behind a load balancer, you need to call `/actuator/refresh` on every instance (in production this is usually automated via Spring Cloud Bus + a message broker, so ONE webhook call fans out to every instance).
- It is a pull, not a push: nothing tells the client a value changed; either an operator/CI calls refresh, or (with Spring Cloud Bus) a config-repo webhook triggers a bus event that every instance is subscribed to.

**Q: How do you secure the config repository / the Config Server itself?**
- Put Config Server behind normal service-to-service auth (mutual TLS, an API gateway with authn, or Spring Security Basic/OAuth2 on the Config Server itself — `spring-boot-starter-security` plus HTTP Basic credentials that clients set via `spring.cloud.config.username/password`).
- Never store secrets in plaintext YAML in the config repo. Use `{cipher}...` encrypted values with Config Server's built-in encryption (`/encrypt`, `/decrypt` endpoints backed by a symmetric key or a KeyStore), or better, delegate actual secrets to a dedicated secrets manager (HashiCorp Vault via `spring-cloud-config-server`'s Vault backend, AWS Secrets Manager, etc.) and keep only non-sensitive settings in Git-backed Config Server.
- Restrict who can push to the Git-backed config repo the same way you'd restrict who can push to a production deploy branch — a bad config change is just as dangerous as a bad code change.

**Q: How would you make Config Server itself highly available?**
Config Server is stateless (state lives in the backing Git repo/native filesystem), so HA is just "run 2+ replicas behind a load balancer or service discovery (Eureka), each pointed at the same backing repo." The interesting failure mode to design for is **the Config Server being briefly unreachable during a client's startup** — this project's `spring.cloud.config.retry.*` (5 attempts, backoff) plus `optional:` on `spring.config.import` (start anyway with local fallback values rather than crash) is exactly that mitigation. In stricter environments you'd flip `fail-fast: true` without `optional:` so a client that truly cannot get its real config refuses to start rather than run with wrong defaults — a deliberate tradeoff between availability and correctness.

**Q: Config Server vs. Kubernetes ConfigMaps/Secrets — how do they compare?**
| | Spring Cloud Config Server | Kubernetes ConfigMap/Secret |
|---|---|---|
| Source of truth | A Git repo (or filesystem/Vault) | Kubernetes API objects (often themselves generated from Git via GitOps) |
| Delivery to app | Pulled over HTTP by the app itself at startup (+ on-demand refresh) | Injected as env vars or mounted files by the kubelet at Pod creation |
| Live update without restart | Yes, via `@RefreshScope` + `/actuator/refresh` (or Spring Cloud Bus fan-out) | Only for **mounted-file** ConfigMaps, and only if the app watches the file for changes itself — env-var-injected ConfigMaps NEVER update without a Pod restart |
| Coupled to a specific runtime? | Spring-specific (though the HTTP contract is simple enough to consume from anywhere) | Platform-level, any container can read env vars/mounted files |
| Encryption of secrets | `{cipher}` values via Config Server's `/encrypt`, or delegate to Vault | `Secret` objects are only base64-encoded by default — need envelope encryption (KMS) or an external secrets operator for real protection |
| Typical real-world combo | Often BOTH are used together: Kubernetes Secrets for the DB password Config Server needs to authenticate to Vault, Config Server for the actual application-tunable config | — |

**Common mistakes to avoid:**
- Forgetting `@RefreshScope` on a `@ConfigurationProperties` bean, then being confused why `/actuator/refresh` "did nothing" — it's not automatic for every bean, only ones explicitly opted in.
- Putting `server.port` or datasource pool-size properties in config-repo and expecting a refresh to change them live — some properties are only read once during context startup regardless of `@RefreshScope`.
- Not exposing `refresh` in `management.endpoints.web.exposure.include` — it is NOT exposed by default, unlike `health`/`info`.
- Treating the native filesystem backend as production-grade — it has no versioning, no audit trail, and no multi-instance consistency guarantees the way a shared Git remote does. Native is for learning/local dev; Git-backed (or Vault-backed for secrets) is for production.
- Assuming one `/actuator/refresh` call updates every replica — without Spring Cloud Bus it only affects the single instance you called.

---

## 13. License

MIT — see [LICENSE](./LICENSE).
