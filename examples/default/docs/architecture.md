# Architecture

## One deployable, many modules

The application is a single Spring Boot JAR and a single Gradle project. Modules are Java packages
under `com.example.springmodulithapplication`, not separate artifacts or services:

```text
com.example.springmodulithapplication
├── Application.java
├── bootstrap/                 explicit composition root
├── identity/                  core business module
├── notifications/             optional business module
├── audit/                     optional business module
├── messaging/                 AMQP and STOMP infrastructure adapters
├── infrastructure/            security, web errors, correlation IDs
└── shared/                    tiny shared kernel (errors only)
```

Each top-level business package is a Spring Modulith application module. Spring Modulith verifies
that modules only use each other's exposed types and that there are no dependency cycles. The test
`ApplicationModulesTest` runs `ApplicationModules.of(Application.class).verify()`.

## Hybrid registration

| Kind | Registration | Activation boundary |
| --- | --- | --- |
| Core module (identity) | Explicit `@Import` in `bootstrap/ApplicationModulesConfiguration` | `IdentityModuleConfiguration` with `@ConditionalOnProperty` |
| Optional module (notifications, audit) | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `*AutoConfiguration` with `@ConditionalOnProperty` |
| Infrastructure adapter | Imported by `MessagingConfiguration` or `InfrastructureConfiguration` | Dedicated `@Configuration` with `@ConditionalOnProperty` |

`Application` is annotated with `@SpringBootApplication` because Spring Modulith's runtime needs
it, but its component scan is narrowed to the `bootstrap` package. Business modules are never
globally scanned. Each module configuration performs a scoped scan of its own internals only when
the module is enabled.

This is why disabling `app.modules.notifications.enabled` removes the notification controller,
service, event listeners, JPA repository, and email adapter from the context instead of merely
skipping their HTTP routes.

## Public APIs and internals

The identity module demonstrates the conventions:

```text
identity/
├── IdentityModuleConfiguration.java   activation boundary
├── IdentityProperties.java            typed configuration
├── api/
│   ├── IdentityApi.java               public use-case interface
│   ├── RegisterUserCommand.java
│   └── UserProfile.java
├── events/
│   └── UserRegisteredEvent.java       public event contract
├── domain/                            entities and repository ports (internal)
├── application/                       services (internal)
├── infrastructure/security/           UserDetailsService adapter (internal)
└── web/                               REST controller (internal)
```

`api` and `events` are declared as named interfaces:

```java
@NamedInterface("api")
package com.example.springmodulithapplication.identity.api;
```

Other modules may use `IdentityApi` and `UserRegisteredEvent`. They may not use
`UserAccountRepository`, `UserAccount`, or `UserAccountService`. The module declaration states
which named interfaces are allowed:

```java
@ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {"identity :: events", "shared :: error"})
package com.example.springmodulithapplication.notifications;
```

The architecture test fails the build when a module crosses one of these boundaries.

## Communication

### Synchronous public API

Use a module's `*Api` interface when the caller needs an immediate result. The interface hides the
implementation, persistence model, and transaction details.

### Application events

Use events for secondary processing and decoupling. Identity publishes `UserRegisteredEvent` after
persisting a user. Notifications and audit consume it asynchronously. Notifications publishes
`NotificationCreatedEvent`, which audit consumes. No module injects another module's repository.

Event listeners use `@ApplicationModuleListener`, which is asynchronous, transactional
(`REQUIRES_NEW`), and executed after the publishing transaction commits. Spring Modulith's event
publication registry stores incomplete publications in the `event_publication` table, so a failed
listener leaves an auditable, retryable record instead of losing the event.

### RabbitMQ

`messaging.rabbitmq` is infrastructure. It consumes public events and writes them to a
transactional outbox; a scheduled relay publishes them with AMQP publisher confirms. The consumer
is idempotent through an inbox table. The AMQP adapter has no business logic and no repository of a
business module.

### STOMP

`messaging.websocket` is infrastructure for client push. It is completely independent from the AMQP
adapter: disabling STOMP does not disable RabbitMQ and disabling RabbitMQ does not disable STOMP.
STOMP messages are not domain events and are not written to the outbox.

## Persistence ownership

Each module owns its tables and Flyway scripts:

| Owner | Table(s) | Migration |
| --- | --- | --- |
| identity | `identity_user_accounts` | `db/migration/identity/V100__identity_create_user_accounts.sql` |
| notifications | `notifications` | `db/migration/notifications/V200__notifications_create_notifications.sql` |
| audit | `audit_entries` | `db/migration/audit/V300__audit_create_audit_entries.sql` |

| messaging (infrastructure) | `messaging_outbox`, `messaging_inbox` | `db/migration/messaging/V400__messaging_create_outbox_and_inbox.sql` |

| Spring Modulith | `event_publication` | `db/migration/infrastructure/V900__infrastructure_event_publication.sql` |

There is no generic shared repository. A module never queries another module's table. Spring Boot's
automatic repository scan is disabled (`spring.data.jpa.repositories.enabled=false`); each module
activation boundary declares its own `@EnableJpaRepositories`. Disabling a module therefore also
removes its repository beans. See [Database and migration ownership](database-migrations.md).

## Cross-cutting infrastructure

`infrastructure` contains:

- `SecurityConfiguration` — stateless HTTP Basic backed by identity's `UserDetailsService`, with
  method security enabled.
- `GlobalExceptionHandler` — RFC 9457 problem responses.
- `CorrelationIdFilter` — request correlation IDs in MDC and response headers.

These are infrastructure, not business capabilities. They do not import business entities or
repositories.

## Failure semantics

- Missing database schema: Flyway fails startup; Hibernate validation also fails startup.
- Invalid configuration: Bean Validation on `@ConfigurationProperties` fails startup.

- RabbitMQ enabled but unreachable or publisher confirms missing: startup fails with a clear message.
- Event publication/consumer failure at runtime: the outbox row remains unpublished and is retried,
  while the AMQP listener retries and then dead-letters.


- STOMP relay selected but the relay port is closed: startup fails with a message naming the
  RabbitMQ STOMP plugin requirement.

- A disabled optional module cannot silently receive traffic because its endpoints and beans are not
  registered at all.

## Further reading

- [Domain events](events.md)
- [Configuration](configuration.md)
- [Conventions and anti-patterns](conventions.md)
