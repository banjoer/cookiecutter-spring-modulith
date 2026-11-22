# RabbitMQ integration

RabbitMQ is an infrastructure adapter, not a business module. It lives in
`{{ cookiecutter.package_name }}.messaging.rabbitmq` and is controlled independently from STOMP.

## Topology

```text
application.events (topic exchange)
└── application.events (durable queue)
    ├── routing key: domain-events
    └── dead-letter exchange: application.events.dlx
        └── application.events.dlq
```

All names are configurable:

```yaml
app:
  integrations:
    rabbitmq:
      enabled: true
      exchange: application.events
      queue-prefix: application
      routing-key: domain-events
```

The adapter declares the exchange, queues, and bindings as Spring beans. Spring Boot's
`AmqpAdmin` declares them when the connection is established.

## Connection configuration

Use standard Spring Boot properties. Do not add duplicate application-specific connection fields.

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:application}
    password: ${RABBITMQ_PASSWORD:}
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

The adapter refuses to start if publisher confirms, returns, or mandatory publishing are missing,
or if the broker is unreachable at startup. This prevents a deployment that appears healthy while
silently failing to deliver integration events.

## Producer

`RabbitMQEventPublisher` publishes an `IntegrationMessage` and waits for a publisher confirm:

- nack → failure;
- unroutable mandatory message → failure;
- timeout → failure.

The normal caller is `OutboxRelay`, not a controller. For operational tooling, inject the publisher
and call `publish(...)` explicitly, or add a dedicated application service.

## Consumer

`RabbitMQEventConsumer` listens on `application.events`. It:

- writes the message id to `messaging_inbox`;
- ignores duplicates by primary key;
- propagates the correlation id into MDC;
- lets exceptions propagate so Spring AMQP retry and dead-lettering apply.

Retry and dead-letter policy use standard Spring Boot properties:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        default-requeue-rejected: false
        retry:
          enabled: true
          max-attempts: 5
          initial-interval: 500ms
          multiplier: 2
          max-interval: 5s
```

After retries are exhausted, the message is rejected without requeue and routed to the DLQ.

## Transactional outbox

`messaging_outbox` is written in the event listener transaction. The relay claims rows using:

```sql
select * from messaging_outbox
where published_at is null
order by created_at
limit ?
for update skip locked
```

`SKIP LOCKED` allows multiple application instances to relay concurrently. Each row carries:

- a stable message id;
- type, aggregate id, occurrence time;
- JSON payload;
- dedupe key;
- correlation id;
- attempts and last error;
- publication timestamp.

The relay is scheduled with `app.integrations.rabbitmq.outbox-poll-interval-ms`. An exception leaves
the row pending and increments `attempts`. The default example does not delete published rows; add a
retention job appropriate for your audit and compliance requirements.

## Idempotent consumer

`messaging_inbox` uses the AMQP message id as its primary key. The consumer checks for an existing
row before inserting. For stricter concurrency, use an insert with `ON CONFLICT DO NOTHING` or catch
the unique violation. Listener retries after a duplicate insert can otherwise dead-letter a message
that was already processed.

## JSON serialization

`JacksonJsonMessageConverter` is registered as the AMQP `MessageConverter` bean using Boot's
Jackson 3 `JsonMapper`. Type information is carried in AMQP headers by Spring AMQP. Keep message
payloads backward compatible; add fields rather than renaming or removing them. Version your event
types when a breaking change is unavoidable.

## Correlation and metadata

Every message includes:

- `messageId` AMQP property;
- `correlationId` AMQP property;
- `type` AMQP property;
- timestamp;
- `x-aggregate-id` header.

The consumer restores the correlation id to MDC for logs. A request correlation id is captured when
the outbox row is created if one exists in MDC; otherwise a new UUID is generated.

## Externalizing domain events

`DomainEventOutboxListener` maps:

| Domain event | Integration type |
| --- | --- |
| `UserRegisteredEvent` | `identity.UserRegistered` |
| `NotificationCreatedEvent` | `notifications.NotificationCreated` |

Set `app.integrations.rabbitmq.externalize-domain-events=false` to keep the adapter available for
explicit producer use without externalizing domain events.

Add new externalized events by adding another `@ApplicationModuleListener` in the adapter. The
adapter depends only on the event's named interface; never on its entities or repositories.

## Testcontainers

`RabbitMQIntegrationTest` starts a real RabbitMQ container and verifies:

1. A user registration publishes an outbox row.
2. The relay publishes it with confirms.
3. The consumer receives it and writes the inbox row.
4. The aggregate id and correlation id are present.
5. STOMP remains disabled without affecting AMQP.

No local RabbitMQ installation is required.

## Disabling the adapter

```bash
APP_INTEGRATIONS_RABBITMQ_ENABLED=false
```

This removes the topology, outbox, producer, consumer, and relay beans. It does not affect STOMP or
the in-process Spring Modulith event registry.

## RabbitMQ and STOMP are different protocols

- AMQP uses port 5672 and is enabled by the RabbitMQ broker.
- STOMP broker relay uses port 61613 and requires the `rabbitmq_stomp` plugin.
- Enabling the RabbitMQ Compose service does not automatically enable the STOMP plugin.
{% if cookiecutter.include_docker_compose|string|lower == 'true' %}
  The supplied compose command enables it explicitly with
  `rabbitmq-plugins enable --offline rabbitmq_stomp`.
{% else %}
  Enable it explicitly on your broker with
  `rabbitmq-plugins enable rabbitmq_stomp` before using relay mode.
{% endif %}
- Disabling STOMP never disables AMQP, and vice versa.
