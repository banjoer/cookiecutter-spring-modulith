# STOMP and WebSocket

STOMP support is a conditional infrastructure adapter in
`com.example.springmodulithapplication.messaging.websocket`. It is independent from the RabbitMQ AMQP
adapter.

## Broker strategies

### Simple broker (development default)

```yaml
app:
  integrations:
    stomp:
      enabled: true
      broker-type: simple
      endpoint: /ws
      application-prefix: /app
      broker-prefix: /topic
```

Spring keeps subscriptions and messages in memory. This is ideal for local development and tests.
It is not durable and does not scale across application instances.

### Broker relay (deployments requiring an external broker)

```yaml
app:
  integrations:
    stomp:
      enabled: true
      broker-type: relay
      endpoint: /ws
      application-prefix: /app
      broker-prefix: /topic
      require-authentication: true
      relay:
        host: ${STOMP_RELAY_HOST}
        port: ${STOMP_RELAY_PORT:61613}
        client-login: ${STOMP_RELAY_CLIENT_LOGIN}
        client-passcode: ${STOMP_RELAY_CLIENT_PASSCODE}
        system-login: ${STOMP_RELAY_SYSTEM_LOGIN}
        system-passcode: ${STOMP_RELAY_SYSTEM_PASSCODE}
```

The relay requires all five relay fields. Startup fails when they are blank or when the relay port
is closed. This is deliberate: a deployment with `broker-type=relay` must not silently fall back to a
local broker.

### RabbitMQ STOMP plugin

RabbitMQ's AMQP support and STOMP support are separate. The STOMP plugin must be enabled on the
broker:

```bash
rabbitmq-plugins enable rabbitmq_stomp
```

The supplied Compose command enables it with `rabbitmq-plugins enable --offline rabbitmq_stomp`
before starting the broker. Port `61613` is exposed. The management UI remains on `15672` and AMQP
on `5672`.


Enabling RabbitMQ AMQP (`app.integrations.rabbitmq.enabled=true`) does **not** enable STOMP, and
disabling STOMP does **not** disable AMQP.

## Endpoint and prefixes

| Property | Default | Meaning |
| --- | --- | --- |
| `endpoint` | `/ws` | WebSocket handshake URL |
| `application-prefix` | `/app` | Client sends to `/app/...` |
| `broker-prefix` | `/topic` | Broker destinations such as `/topic/chat` |
| `user-destination-prefix` | `/user` | User-specific destinations |

The example controller sends to the configured broker prefix dynamically, so changing the prefix
does not require changing Java code.

## Example flow

1. Connect to `ws://localhost:8080/ws`.
2. Authenticate in the STOMP CONNECT frame when `require-authentication=true`.
3. Subscribe to `/topic/chat`.
4. Send a JSON body to `/app/chat.send`:

```json
{"content":"hello modular monolith"}
```

5. Receive:

```json
{
  "author": "alice",
  "content": "hello modular monolith",
  "sentAt": "2026-11-22T16:00:00Z"
}
```

## Authentication

`StompAuthenticationInterceptor` reads the HTTP Basic `Authorization` **native header** from the
STOMP CONNECT frame and authenticates through the same `AuthenticationManager` used by REST. It
sets the authenticated principal for the session. The controller receives it as `Principal`.

For browser clients, prefer a secure WebSocket (`wss`), an OIDC token in CONNECT, and a
subscription authorization policy appropriate for your domain. The starter demonstrates the
integration point; it is not a complete multi-tenant authorization model.

When `require-authentication=false`, unauthenticated sessions are accepted and the chat author is
`anonymous`. Use this only for local experiments and tests.

## CORS and origins

`allowed-origins` controls the WebSocket handshake origin patterns:

```yaml
app:
  integrations:
    stomp:
      allowed-origins:
        - https://app.example.com
```

Do not use `*` in production with credential-bearing clients.

## Server-side validation

The example controller rejects blank content and content longer than 1000 characters. Add domain
authorization, rate limits, and message size limits appropriate to your use case.

## Testing

`StompIntegrationTest` starts PostgreSQL and the application on a random port, registers a real
user, connects with a Basic CONNECT header, subscribes to `/topic/chat`, sends a message, and
asserts the authenticated author. It uses the simple broker, so no external STOMP broker is
required.

To test a relay manually:

```bash
STOMP_BROKER_TYPE=relay \
STOMP_RELAY_HOST=localhost \
STOMP_RELAY_PORT=61613 \
STOMP_RELAY_CLIENT_LOGIN=application \
STOMP_RELAY_CLIENT_PASSCODE=... \
STOMP_RELAY_SYSTEM_LOGIN=application \
STOMP_RELAY_SYSTEM_PASSCODE=... \
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Production guidance

- Use a relay for multi-instance deployments so subscriptions are not trapped in one JVM.
- Terminate TLS and use `wss`.
- Authenticate CONNECT and authorize SUBSCRIBE/SEND destinations explicitly.
- Limit message size and connection counts at the edge.
- Monitor broker connections and dead subscriptions.
- Keep the simple broker for local development only.

## Disabling STOMP

```bash
APP_INTEGRATIONS_STOMP_ENABLED=false
```

The WebSocket endpoint, broker configuration, interceptors, controller, and session listeners are
not registered. RabbitMQ AMQP remains unaffected.
