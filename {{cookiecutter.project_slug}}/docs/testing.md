# Testing strategy

{% if cookiecutter.include_testcontainers|string|lower != 'true' -%}
This project was generated without Testcontainers. It includes unit and architecture tests only;
add Docker-backed integration tests to verify persistence, security, and event delivery.
{% endif %}
## Test layers

| Layer | Example | Docker required |
| --- | --- | --- |
| Domain unit | `UserAccountTest` | No |
| Architecture | `ApplicationModulesTest` | No |
{% if cookiecutter.include_testcontainers|string|lower == 'true' %}
| Module integration | `IdentityIntegrationTest`, `NotificationsIntegrationTest`, `AuditIntegrationTest` | PostgreSQL |
| Activation | `ActivationIntegrationTest` | PostgreSQL |
{% if cookiecutter.include_rabbitmq|string|lower == 'true' %}
| Broker integration | `RabbitMQIntegrationTest` | PostgreSQL + RabbitMQ |
{% endif %}
{% if cookiecutter.include_stomp|string|lower == 'true' %}
| WebSocket integration | `StompIntegrationTest` | PostgreSQL |
{% endif %}
| HTTP security | `SecurityIntegrationTest` | PostgreSQL |
| Optional email | `EmailNotificationsIntegrationTest` | PostgreSQL |
{% endif %}

## Commands

```bash
./gradlew test
./gradlew test --tests '*ApplicationModulesTest'
{% if cookiecutter.include_testcontainers|string|lower == 'true' %}
./gradlew test -PskipIntegrationTests
{% endif %}
{% if cookiecutter.include_rabbitmq|string|lower == 'true' and cookiecutter.include_testcontainers|string|lower == 'true' %}
./gradlew test --tests '*RabbitMQIntegrationTest'
{% endif %}
```

{% if cookiecutter.include_testcontainers|string|lower == 'true' -%}
Integration tests are tagged `integration` through the `@IntegrationTest` meta-annotation. The Gradle
build excludes that tag when `-PskipIntegrationTests` is supplied, so architecture and unit tests
still run on a machine without Docker.
{% else -%}
The included tests do not need Docker or external infrastructure.
{% endif %}

## Architecture test

```java
ApplicationModules.of(Application.class).verify();
```

This fails on:

- a module using another module's internal type;
- a dependency on an unnamed package;
- dependency cycles;
- undeclared named-interface access.

The test also asserts that expected top-level modules are discovered.

{% if cookiecutter.include_testcontainers|string|lower == 'true' %}
## Testcontainers

`AbstractPostgresIntegrationTest` starts one PostgreSQL 17 container per test JVM. The container is
shared by all test classes and stopped by Testcontainers' Ryuk cleanup when the JVM exits.
`AbstractMessagingIntegrationTest` adds a RabbitMQ 4 management container for AMQP tests.

Dynamic properties override `spring.datasource.*` and `spring.rabbitmq.*`. No local PostgreSQL or
RabbitMQ installation is used, and tests do not depend on developer machine state.

Spring contexts are cached by property set. Tests use unique usernames and aggregate ids so data
from one context does not make another test flaky.

## Enabled/disabled module tests

`ActivationIntegrationTest` starts the application with identity, notifications, and audit disabled
and asserts that their public API beans are absent. This is stronger than checking a property value:
it proves the conditional configuration boundaries actually prevent bean registration.

Add an enabled counterpart for each optional module. For example,
`NotificationsIntegrationTest` enables notifications, registers a user, waits for the asynchronous
event, and asserts the notification projection.

## Event tests

Asynchronous listeners use `@ApplicationModuleListener`. Use Awaitility rather than `Thread.sleep`:

```java
await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
        assertThat(audit.recent(100))
                .anySatisfy(entry -> assertThat(entry.aggregateId()).isEqualTo(user.id())));
```

Assert observable outcomes: public API results, database rows, inbox/outbox state, or broker
delivery. Do not assert thread timing or ordering that the design does not guarantee.

{% if cookiecutter.include_rabbitmq|string|lower == 'true' %}
## RabbitMQ tests

`RabbitMQIntegrationTest` exercises the full path:

1. Register a user through the identity API.
2. Wait for the outbox listener to persist a row.
3. Wait for the scheduled relay to publish and receive a confirm.
4. Wait for the consumer to write the inbox row.
5. Assert the aggregate id and correlation id.
6. Assert that STOMP did not become active.

Because the test waits for asynchronous work, it uses generous but bounded timeouts. The relay poll
interval is shortened with `app.integrations.rabbitmq.outbox-poll-interval-ms=200`.
{% endif %}

{% if cookiecutter.include_stomp|string|lower == 'true' and cookiecutter.include_testcontainers|string|lower == 'true' %}
## STOMP tests

`StompIntegrationTest` runs the web server on a random port, registers a user, connects with an
HTTP Basic CONNECT header, subscribes to the configured broker prefix, sends a message, and asserts
the authenticated author. It uses the simple broker and does not require an external STOMP broker.
{% endif %}
{% endif %}

## Determinism rules

- Use unique identities and aggregate ids.
- Use Awaitility with bounded timeouts for asynchronous work.
- Do not rely on test execution order.
- Do not depend on a locally running service.
- Keep `maxParallelForks = 1` unless you have verified container and database isolation.
- Pin container images (`postgres:17-alpine`, `rabbitmq:4-management`).

{% if cookiecutter.include_testcontainers|string|lower == 'true' %}
## CI

A CI job needs Docker (Testcontainers) and a JDK matching the project toolchain. A typical job runs:

```bash
./gradlew clean test
```

If Docker is unavailable in a fast feedback job, run `./gradlew test -PskipIntegrationTests` there
and run the full suite in a separate Docker-capable job.
{% endif %}
