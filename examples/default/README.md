# Spring Modulith Application

A modular monolith built with Spring Boot and Spring Modulith.

This is a single-deployable modular monolith built with **Java 21**, **Spring Boot 4.2.0**, **Spring Modulith**, PostgreSQL, and Flyway, RabbitMQ, STOMP/WebSocket, and Testcontainers. It was generated with the `cookiecutter-spring-modulith` template.

The application is a monolith at deployment time and a set of explicitly bounded modules at development time. There is no Gradle multi-project build and no second deployable.

## Quick start

### Requirements

- Java 21 (a local JDK; the Gradle toolchain can also download it)
- Docker Engine with Compose v2
- PostgreSQL
- No local PostgreSQL or RabbitMQ installation is required if you use Compose or Testcontainers


### 1. Configure local values

```bash
cp .env.example .env
# Edit .env. The checked-in example values are for local development only.
```

### 2. Start the infrastructure

```bash
docker compose up -d postgres rabbitmq
```


RabbitMQ's management UI is at <http://localhost:15672>. The image explicitly enables
`rabbitmq_stomp` on port 61613. The AMQP listener is on port 5672.



### 3. Run the application

```bash
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Or run the entire stack, including the application, in Docker:

```bash
docker compose up -d --build
```


The application listens on <http://localhost:8080>. Readiness is exposed at
`/actuator/health/readiness` and liveness at `/actuator/health/liveness`.

### 4. Smoke test the API

Registration is public. All other `/api/**` endpoints require authentication. The starter uses
stateless HTTP Basic with BCrypt password hashes stored in PostgreSQL. Replace it with your
organization's OIDC, token, or mTLS setup for production.

```bash
curl -i -X POST http://localhost:8080/api/identity/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"correct-horse-battery"}'

curl -u alice:correct-horse-battery http://localhost:8080/api/identity/me
curl -u alice:correct-horse-battery http://localhost:8080/api/notifications
curl -u alice:correct-horse-battery http://localhost:8080/api/audit
curl http://localhost:8080/actuator/health/readiness
```



Registering a user publishes a `UserRegisteredEvent`. The RabbitMQ adapter writes it to the
transactional outbox, relays it with publisher confirms, and the example consumer stores it in the
idempotent inbox table. Inspect it with:

```bash
docker compose exec postgres psql -U application -d application \
  -c 'select type, published_at, attempts from messaging_outbox;'
docker compose exec postgres psql -U application -d application \
  -c 'select type, aggregate_id, received_at from messaging_inbox;'
```




A browser or STOMP client can connect to `ws://localhost:8080/ws`. With the default
`require-authentication: true`, CONNECT must carry an HTTP Basic `Authorization` native header.
Send to `/app/chat.send` and subscribe to `/topic/chat`. See [STOMP configuration](docs/stomp.md).


## What is included

| Area | Implementation |
| --- | --- |
| Packaging | One Gradle project, one Spring Boot application |
| Module boundaries | Spring Modulith `@ApplicationModule`, named interfaces, `ApplicationModules.verify()` |
| Core module registration | Explicit imports in `bootstrap/ApplicationModulesConfiguration` |
| Optional module registration | Spring Boot auto-configuration with `@ConditionalOnProperty` |
| Runtime activation | `app.modules.*.enabled` and `app.integrations.*.enabled` |
| Persistence | PostgreSQL, Spring Data JPA, Flyway, module-owned tables |
| Reliable messaging | Transactional outbox, publisher confirms, idempotent inbox, retry and DLQ |
| WebSocket | STOMP with either an in-memory simple broker or a broker relay |
| Security | Stateless Spring Security, BCrypt, method security, no hardcoded users |
| Observability | Actuator, Micrometer, Prometheus, correlation IDs, OpenTelemetry OTLP export |
| Testing | Unit tests and architecture tests, activation tests, PostgreSQL and RabbitMQ Testcontainers |

### Modules

- **identity** — core module. Owns users, password hashes, registration, and `UserRegisteredEvent`.
- **notifications** — optional module. Persists notifications, optionally sends email, publishes
  `NotificationCreatedEvent`.
- **audit** — optional module. Asynchronously records public events in its own table.
- **messaging** — infrastructure composition root with an independent RabbitMQ AMQP adapter and an independent STOMP/WebSocket adapter. It is not a business domain.
- **infrastructure** — cross-cutting security, web error handling, and correlation IDs.
- **shared** — a tiny shared kernel for cross-module exception types. No repositories or entities.

## Activating and deactivating capabilities

Cookiecutter options determine which adapters and dependencies are generated. Spring configuration
determines what is active at runtime. Optional module source code is always present so the same
artifact can be promoted between environments with configuration.

```yaml
app:
  modules:
    identity:
      enabled: true
    notifications:
      enabled: true
    audit:
      enabled: false

  integrations:

    rabbitmq:
      enabled: true


    stomp:
      enabled: true


```

Environment variables use Spring Boot's relaxed binding, for example:

```bash
APP_MODULES_NOTIFICATIONS_ENABLED=false
APP_MODULES_AUDIT_ENABLED=false

APP_INTEGRATIONS_RABBITMQ_ENABLED=false


APP_INTEGRATIONS_STOMP_ENABLED=false
STOMP_BROKER_TYPE=relay

```

All module configuration classes are activation boundaries. Disabling a module prevents its
services, controllers, listeners, schedulers, repositories, and adapters from being registered.
See [configuration](docs/configuration.md) and the `ActivationIntegrationTest`.

## Tests

```bash
./gradlew test
```

Integration tests start PostgreSQL and RabbitMQ with Testcontainers. No local database or broker is needed, but Docker must be available. To run only fast tests:

```bash
./gradlew test -PskipIntegrationTests
```

The suite covers unit behavior, Flyway migrations, Spring Modulith architecture verification,
enabled and disabled module scenarios, security, asynchronous events, the RabbitMQ outbox/inbox round trip, and the authenticated STOMP chat flow.


## Configuration

Frequently used properties:

| Property | Default | Purpose |
| --- | --- | --- |
| `app.modules.identity.enabled` | `true` | Activate identity beans and endpoints |
| `app.modules.identity.registration-enabled` | `true` | Allow public self-registration |
| `app.modules.identity.minimum-password-length` | `12` | Minimum password length |
| `app.modules.notifications.enabled` | `true` | Activate notification listeners and endpoints |
| `app.modules.notifications.email.enabled` | `false` | Enable the optional email adapter |
| `app.modules.notifications.email.from` | empty | Required when email is enabled |
| `app.modules.audit.enabled` | `true` | Activate audit listeners and endpoint |
| `app.modules.audit.max-page-size` | `100` | Cap audit query size |

| `app.integrations.rabbitmq.enabled` | `true` | Activate AMQP topology, outbox, producer, consumer |
| `app.integrations.rabbitmq.exchange` | `application.events` | Durable topic exchange |
| `app.integrations.rabbitmq.queue-prefix` | `application` | Queue name prefix |
| `app.integrations.rabbitmq.routing-key` | `domain-events` | Event routing key |
| `app.integrations.rabbitmq.externalize-domain-events` | `true` | Write public events to the outbox |
| `app.integrations.rabbitmq.outbox-poll-interval-ms` | `2000` | Relay delay |
| `app.integrations.rabbitmq.outbox-batch-size` | `50` | Maximum rows claimed per relay run |


| `app.integrations.stomp.enabled` | `true` | Activate the WebSocket endpoint and STOMP broker |
| `app.integrations.stomp.endpoint` | `/ws` | WebSocket handshake endpoint |
| `app.integrations.stomp.application-prefix` | `/app` | Client-to-server destination prefix |
| `app.integrations.stomp.broker-prefix` | `/topic` | Broker destination prefix |
| `app.integrations.stomp.broker-type` | `simple` | `simple` or `relay` |
| `app.integrations.stomp.require-authentication` | `true` | Require authenticated STOMP sessions |


Database and broker connection details use standard Spring Boot properties:
`spring.datasource.*` and, when RabbitMQ is included, `spring.rabbitmq.*`. Secrets are read from
environment variables in `application.yml`; no production password is committed.

Startup fails with an actionable message when an enabled capability is incomplete. Examples include
enabling email without `spring.mail.host`, enabling RabbitMQ without publisher confirms or without a
reachable broker, and selecting a STOMP relay whose port is closed.

See [configuration](docs/configuration.md) for the complete reference.

## Documentation

- [Architecture and module boundaries](docs/architecture.md)
- [Adding a new module](docs/adding-a-module.md)
- [Configuration and runtime activation](docs/configuration.md)
- [Domain events and reliable delivery](docs/events.md)
- [Database and migration ownership](docs/database-migrations.md)
- [Testing strategy](docs/testing.md)
- [Local development and Docker Compose](docs/local-development.md)
- [Production deployment](docs/production.md)
- [Conventions and anti-patterns](docs/conventions.md)
- [RabbitMQ integration](docs/rabbitmq.md)
- [STOMP and broker strategy](docs/stomp.md)

## Production checklist

- Replace HTTP Basic with your identity provider and enforce TLS.
- Store passwords and broker credentials in a secret manager, not in `.env` or images.
- Apply database migrations as a controlled deployment step.
- Run more than one application instance only after reviewing the outbox claim strategy.
- Scrape `/actuator/prometheus`, ship structured logs, and export traces to your observability backend.

- Configure RabbitMQ as a quorum queue deployment for production and monitor the dead-letter queue.

- Review [production deployment](docs/production.md) before going live.
