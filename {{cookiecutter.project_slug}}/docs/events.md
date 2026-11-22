# Domain events and reliable delivery

## Public event contracts

Events that cross a module boundary live in a named interface and contain only immutable data:

```java
@NamedInterface("events")
package {{ cookiecutter.package_name }}.identity.events;

public record UserRegisteredEvent(
        UUID userId,
        String username,
        String email,
        Instant registeredAt) {
}
```

Events are past-tense facts. They do not expose entities, repositories, or lazy JPA relations.

## Publishing

Publish after the state change and inside the transaction that owns it:

```java
@Transactional
public UserProfile register(RegisterUserCommand command) {
    UserAccount account = repository.save(/* ... */);
    events.publishEvent(new UserRegisteredEvent(
            account.getId(), account.getUsername(), account.getEmail(), Instant.now()));
    return toProfile(account);
}
```

Spring Modulith records the publication for every `@ApplicationModuleListener` in the same
transaction. If the transaction rolls back, no listener runs. If the listener fails after commit,
the registry leaves an incomplete publication that can be inspected and retried.

## Consuming

Use `@ApplicationModuleListener` for secondary processing:

```java
@ApplicationModuleListener
void onUserRegistered(UserRegisteredEvent event) {
    repository.save(new Notification(/* ... */));
    events.publishEvent(new NotificationCreatedEvent(/* ... */));
}
```

The annotation is asynchronous and runs in a new transaction after the publisher commits. Do not
add `@Transactional` to the same method: Spring Framework rejects a
`@TransactionalEventListener` that is also `@Transactional` unless the propagation is
`REQUIRES_NEW` or `NOT_SUPPORTED`. `@ApplicationModuleListener` already supplies `REQUIRES_NEW`.

Use `@TransactionalEventListener(phase = AFTER_COMMIT)` only when you need synchronous after-commit
execution or a different transaction strategy.

## Failure handling

- The event publication registry table is `event_publication`.
- Incomplete publications remain visible and can be resubmitted using Spring Modulith's
  `IncompleteEventPublications` or `FailedEventPublications` APIs.
- An asynchronous listener failure does not roll back the publisher's business transaction.
- Do not catch and swallow listener exceptions. Either handle them deliberately or let the registry
  record the failure.

This is at-least-once delivery for in-process listeners. Listeners must be idempotent when they
perform non-idempotent side effects. The audit and notification inserts are naturally keyed; add a
unique constraint to your own projections if retries can create duplicates.

{% if cookiecutter.include_rabbitmq|string|lower == 'true' -%}
## Reliable external publication

Spring events are not a broker deliverability guarantee. For critical integration messages this
project implements a transactional outbox:

1. An `@ApplicationModuleListener` in `messaging.rabbitmq.outbox` observes a public event and
   inserts an outbox row in the listener transaction.
2. A scheduled relay claims pending rows with `FOR UPDATE SKIP LOCKED`.
3. The relay publishes with AMQP publisher confirms and mandatory routing.
4. Only an acknowledged, routable message is marked published.
5. The consumer stores the message id in an inbox table, making redelivery idempotent.

The outbox row is the source of truth until RabbitMQ confirms it. A broker outage therefore delays
delivery rather than losing the event. For very high throughput, use a dedicated outbox library or
the Spring Modulith externalization outbox integrations instead of the included example relay.
{% else -%}
## External publication

This project was generated without RabbitMQ. Spring Modulith still provides reliable in-process
delivery and failure visibility through its event publication registry. Add a transactional outbox
or the broker's native reliable-publish mechanism before forwarding critical events to an external
broker.
{% endif %}

## Testing events

Use Awaitility because listeners are asynchronous:

```java
await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
        assertThat(notifications.findByRecipient(username)).hasSize(1));
```

Assert the public API or database projection, not an implementation detail. Add tests with the
consuming module enabled and disabled:

- enabled: the listener bean exists and produces the projection;
- disabled: the listener and projection beans are absent and the application still starts.

## Anti-patterns

- Calling another module's repository from an event listener.
- Publishing an event before the owning transaction commits.
- Using events for request/response flows that need a synchronous answer.
- Making an event listener block on a remote broker without an outbox or retry strategy.
- Treating STOMP messages as domain events. STOMP is a transport for connected clients.
