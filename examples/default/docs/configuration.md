# Configuration and runtime activation

## Two kinds of configuration

| Layer | Decides | Examples |
| --- | --- | --- |
| Cookiecutter options | Which source, dependencies, and compose services are generated | RabbitMQ adapter, STOMP adapter, Docker Compose, Testcontainers |
| Spring configuration | Which generated capabilities are active at runtime | `app.modules.audit.enabled=false`, `app.integrations.rabbitmq.enabled=false` |

Optional module source is always generated. That is deliberate: the same JAR can be promoted from
a staging environment with notifications enabled to a deployment where notifications are disabled.
No build-time profile or separate artifact is required.

## Module properties

### identity

| Property | Default | Validation | Description |
| --- | --- | --- | --- |
| `app.modules.identity.enabled` | `true` | boolean | Activates the module and its beans |
| `app.modules.identity.registration-enabled` | `true` | boolean | Allows public `POST /api/identity/users` |
| `app.modules.identity.minimum-password-length` | `12` | 8–128 | Password policy enforced by the service |

### notifications

| Property | Default | Validation | Description |
| --- | --- | --- | --- |
| `app.modules.notifications.enabled` | `true` | boolean | Activates listeners, controller, repository |
| `app.modules.notifications.email.enabled` | `false` | boolean | Activates the email adapter |
| `app.modules.notifications.email.from` | empty | email, required when enabled | Sender address |
| `app.modules.notifications.email.subject-prefix` | `[spring-modulith-application] ` | max 80 | Subject prefix |

Email delivery uses Spring Boot's standard `spring.mail.*` properties. When
`app.modules.notifications.email.enabled=true` and no `JavaMailSender` exists, startup fails with an
explicit message instead of silently dropping email.

### audit

| Property | Default | Validation | Description |
| --- | --- | --- | --- |
| `app.modules.audit.enabled` | `true` | boolean | Activates audit listeners and endpoint |
| `app.modules.audit.max-page-size` | `100` | 1–1000 | Upper bound for audit queries |


## RabbitMQ properties

| Property | Default | Description |
| --- | --- | --- |
| `app.integrations.rabbitmq.enabled` | `true` | Activates topology, outbox, producer, consumer |
| `app.integrations.rabbitmq.exchange` | `application.events` | Durable topic exchange |
| `app.integrations.rabbitmq.queue-prefix` | `application` | Main and dead-letter queue prefix |
| `app.integrations.rabbitmq.routing-key` | `domain-events` | Routing key for externalized events |
| `app.integrations.rabbitmq.externalize-domain-events` | `true` | Write public events to the outbox |
| `app.integrations.rabbitmq.outbox-poll-interval-ms` | `2000` | Relay delay in milliseconds |
| `app.integrations.rabbitmq.outbox-batch-size` | `50` | Rows claimed per relay run |
| `app.integrations.rabbitmq.publisher-confirm-timeout-seconds` | `10` | Confirm wait per message |

Connection settings are **not duplicated**. Use standard Spring Boot properties:
`spring.rabbitmq.host`, `spring.rabbitmq.port`, `spring.rabbitmq.username`,
`spring.rabbitmq.password`, TLS settings, and listener retry settings.

The adapter requires these reliability settings; startup fails if they are missing:

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

See [RabbitMQ](rabbitmq.md).



## STOMP properties

| Property | Default | Description |
| --- | --- | --- |
| `app.integrations.stomp.enabled` | `true` | Activates the WebSocket endpoint and broker |
| `app.integrations.stomp.endpoint` | `/ws` | Handshake endpoint |
| `app.integrations.stomp.application-prefix` | `/app` | Client-to-server destination prefix |
| `app.integrations.stomp.broker-prefix` | `/topic` | Broker destination prefix |
| `app.integrations.stomp.user-destination-prefix` | `/user` | User destination prefix |
| `app.integrations.stomp.broker-type` | `simple` | `simple` or `relay` |
| `app.integrations.stomp.require-authentication` | `true` | Require an authenticated session |
| `app.integrations.stomp.allowed-origins` | `http://localhost:8080` | WebSocket origin allow-list |
| `app.integrations.stomp.relay.host` | empty | External STOMP broker host |
| `app.integrations.stomp.relay.port` | `61613` | External STOMP broker port |
| `app.integrations.stomp.relay.client-login` | empty | Relay client login |
| `app.integrations.stomp.relay.client-passcode` | empty | Relay client passcode |
| `app.integrations.stomp.relay.system-login` | empty | Relay system login |
| `app.integrations.stomp.relay.system-passcode` | empty | Relay system passcode |

Relay credentials are required only when `broker-type=relay` and are validated when the
configuration is bound. See [STOMP](stomp.md).


## Standard Spring Boot properties

Do not duplicate standard properties in application-specific classes:

- Datasource: `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- JPA: `spring.jpa.*`
- RabbitMQ connection and listener retry: `spring.rabbitmq.*`

- OpenTelemetry endpoint: `management.opentelemetry.otlp.endpoint`
- Tracing sampling: `management.tracing.sampling.probability`

- Actuator and health: `management.*`

Application-specific classes only own concepts that do not exist in Spring Boot: module toggles,
topology naming, outbox behavior, STOMP broker strategy, and business policies.

## Environment variable examples

```bash
APP_MODULES_IDENTITY_ENABLED=true
APP_MODULES_NOTIFICATIONS_ENABLED=false
APP_MODULES_AUDIT_ENABLED=false

APP_INTEGRATIONS_RABBITMQ_ENABLED=false
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
RABBITMQ_USERNAME=application
RABBITMQ_PASSWORD=...


APP_INTEGRATIONS_STOMP_ENABLED=true
APP_INTEGRATIONS_STOMP_BROKER_TYPE=relay
STOMP_RELAY_HOST=rabbitmq
STOMP_RELAY_PORT=61613

DB_URL=jdbc:postgresql://postgres:5432/application
DB_USERNAME=application
DB_PASSWORD=...
```

Spring Boot maps `APP_MODULES_FOO_ENABLED` to `app.modules.foo.enabled` using relaxed binding.
Compose uses the shorter `DB_*` names because `application.yml` explicitly maps them.

## Validation and startup failures

Configuration is validated with Jakarta Bean Validation. The application fails fast when:

- the STOMP relay is selected but any required relay field is blank;
- email notifications are enabled but no mail sender is configured;

- RabbitMQ is enabled but publisher confirms/returns/mandatory publishing are missing;
- RabbitMQ is enabled but the broker cannot be reached at startup;


- the STOMP relay port is unreachable at startup;

- the database is unreachable or Flyway/Hibernate validation fails.

This is intentional. An integration that was requested is never silently disabled because its
configuration is incomplete.

## Disabling capabilities safely

- Disabling a module removes its beans; its database schema remains. This is usually desirable for
  reversible toggles. Remove migrations only with a deliberate schema-retirement plan.
- Disabling RabbitMQ stops the outbox relay and topology beans. Domain event publications for audit
  and notifications still use the in-process Spring Modulith registry.
- Disabling STOMP does not disable AMQP.
- Disabling identity makes protected endpoints fail closed; registration is no longer mapped.
  Notifications and audit still start, but no new identity events are produced.
