# Architectural conventions and anti-patterns

## Do

### Keep one deployable

Use a single Spring Boot application and a single Gradle project. Module boundaries belong in Java
packages and Spring Modulith metadata, not in deployment topology.

### Treat each business capability as a module

A module owns:

- its public API;
- its events;
- its domain model and repository ports;
- its application services;
- its web adapters;
- its persistence model and migrations;
- its tests.

### Expose only named interfaces

```java
@NamedInterface("api")
package com.example.springmodulithapplication.orders.api;

@NamedInterface("events")
package com.example.springmodulithapplication.orders.events;
```

Everything else is internal by default. Declare allowed dependencies explicitly.

### Use configuration classes as activation boundaries

A module has one clear conditional configuration. Conditions belong there, not scattered across
services, controllers, and repositories.

### Use typed, validated configuration

Use `@ConfigurationProperties` with Jakarta validation. Add defaults and document each property.
Use environment variables for deployment-specific values.

### Use events for secondary work

Persist the primary state, publish a public event, and let other modules react asynchronously. Use
Spring Modulith's publication registry for failure visibility.

### Use reliable delivery for external messages

For critical integration events, use the transactional outbox and publisher confirms. Make
consumers idempotent. Do not assume a message is delivered because a send method returned.

### Own your schema

Use a version range per module and never modify an applied migration. Keep Hibernate in validate
mode.

### Test both enabled and disabled states

A conditional module that cannot start disabled is not optional. Test the absence of its beans, not
only the presence.

## Don't

### Don't inject another module's repository

```java
// Wrong: orders reaches into identity internals.
public class OrderService {
    private final UserAccountRepository users; // cross-module internal dependency
}
```

Use `IdentityApi` for a required synchronous answer or listen to `UserRegisteredEvent` for secondary
processing.

### Don't share entities

An entity is a persistence detail. Expose a record or immutable view. Never return a lazy JPA entity
from a module API.

### Don't create a generic shared repository layer

`shared` is a tiny kernel for truly cross-cutting types. It must not contain `BaseEntity`,
`BaseRepository`, or database utilities used by every module.

### Don't scan the whole application root

A broad `@ComponentScan` undermines runtime module deactivation. Keep the application scan narrowed
to bootstrap and let each module scan only its own internals.

### Don't scatter `@ConditionalOnProperty`

One activation class per module/adapter. If a condition changes, it should change in one place.

### Don't mix infrastructure and domain logic

RabbitMQ and STOMP adapters translate transport concerns. They do not own business rules, entities,
or repositories. Business modules do not depend on messaging internals.

### Don't use STOMP as an event bus

STOMP is for connected clients. Domain events are for module decoupling. Use RabbitMQ for durable
integration between systems.

### Don't duplicate standard Spring properties

Do not add `app.rabbitmq.host` when `spring.rabbitmq.host` already exists. Do not add a custom
datasource block. Wrap only concepts that Spring Boot does not model.

### Don't silently degrade

If a capability is enabled but its infrastructure is missing, fail startup with a clear message.
Silent fallback hides production incidents.

### Don't use `ddl-auto=update`

Schema changes belong in Flyway. Hibernate validation catches mapping drift.

### Don't make event listeners non-idempotent

The registry and broker can redeliver. Use stable ids, unique constraints, or inbox rows.

### Don't share a transaction across modules

A module's transaction ends at its API boundary. Use events or an explicitly designed saga for
multi-step workflows. Cross-module database transactions create hidden coupling.

## Naming

- Modules: singular business capability, lowercase package (`identity`, `billing`, `inventory`).
- Public API: `<Module>Api` in `<module>.api`.
- Events: past tense, immutable records, in `<module>.events`.
- Configuration: `<Module>Properties` in the module root.
- Activation: `<Module>ModuleConfiguration` for core, `<Module>AutoConfiguration` for optional.
- Migrations: `V<range>__<module>_<description>.sql`.

## Review checklist

- [ ] Does the change belong to exactly one module?
- [ ] Are new public types in a named interface?
- [ ] Does an architecture test prove the boundary?
- [ ] Is configuration typed, validated, and documented?
- [ ] Is activation centralized and tested both ways?
- [ ] Does a new table have an owner and a migration?
- [ ] Is external messaging reliable and idempotent?
- [ ] Are secrets environment-provided?
- [ ] Do tests pass without a local PostgreSQL or RabbitMQ installation?
