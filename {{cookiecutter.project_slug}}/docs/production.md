# Production deployment

## Build

```bash
./gradlew clean bootJar
java -jar build/libs/application.jar --spring.profiles.active=prod
```

The Dockerfile produces the same JAR and runs it as a non-root user:

```bash
docker build -t {{ cookiecutter.project_slug }}:release .
```

## Configuration

Production configuration comes from environment variables, a secret manager, or your platform's
configuration service. The application image should be environment-agnostic.

Required values:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
{% if cookiecutter.include_rabbitmq|string|lower == 'true' %}
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- RabbitMQ TLS properties if the broker is not on a trusted private network
{% endif %}
{% if cookiecutter.include_stomp|string|lower == 'true' %}
- STOMP relay host and credentials if `STOMP_BROKER_TYPE=relay`
{% endif %}
{% if cookiecutter.include_observability|string|lower == 'true' %}
- `OTEL_EXPORTER_OTLP_ENDPOINT` and sampling configuration
{% endif %}

Do not bake credentials into images, source, Compose files, or documentation. The `.env.example`
file is for local development only.

## Security

The starter provides stateless HTTP Basic with BCrypt hashes and no hardcoded users. Before
production:

- Terminate TLS and require HTTPS/WSS.
- Replace Basic authentication with OIDC, a signed token, mTLS, or your platform's authentication.
- Configure CSRF appropriately for browser session flows. The stateless API disables CSRF; do not
  copy that choice blindly to a cookie-authenticated UI.
- Restrict `allowed-origins` for STOMP.
- Add authorization rules for every endpoint and method. Method security is enabled, but the
  included rules are examples.
- Rotate database and broker credentials.
- Run the container as a non-root user (the supplied Dockerfile does).
- Keep dependencies patched and scan images.

## Database

- Use a managed PostgreSQL service with backups, PITR, and connection limits.
- Run Flyway as part of the deployment or as a controlled pre-deployment Job. Do not let multiple
  instances race migrations without a Flyway lock strategy appropriate for your platform.
- Use `spring.jpa.hibernate.ddl-auto=validate`; never `update` in production.
- Size the Hikari pool against the database and the number of application instances.
- Test restores, not just backups.

## Health and readiness

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- General health: `/actuator/health`

The readiness group includes database health. {% if cookiecutter.include_rabbitmq|string|lower == 'true' %}RabbitMQ health is enabled only when the AMQP integration is
enabled, using `management.health.rabbit.enabled=${app.integrations.rabbitmq.enabled}`.{% endif %}
Configure your orchestrator to use readiness for traffic and liveness only for restart decisions.

## Observability

- Prometheus scrape: `/actuator/prometheus` (authenticated; expose it on an internal management
  network or permit a dedicated actuator path in your security policy).
{% if cookiecutter.include_observability|string|lower == 'true' %}
- Tracing: OpenTelemetry OTLP export through `management.opentelemetry.otlp.endpoint`.
- Sampling: `management.tracing.sampling.probability`; lower it in high-traffic environments.
- Logs: the `prod` profile emits ECS JSON to stdout. Ship stdout to your log platform.
{% endif %}
- Correlation IDs are included in error responses and MDC where the logging pattern supports them.
- Add alerts for outbox backlog, inbox failures, dead-letter queue depth, HTTP error rate, and
  database saturation.

## RabbitMQ

- Use durable exchanges and queues (the template does).
- Prefer quorum queues for production durability and replication. Change the queue type
  deliberately and test the behavior.
- Monitor the dead-letter queue and alert on growth.
- Set resource limits and disk alarms.
- Use TLS and least-privilege broker users.
- The provided outbox relay is safe across instances through `SKIP LOCKED`, but confirm throughput
  and retention meet your requirements. Add a retention job for published rows.

## Scaling

The application is stateless apart from its database and broker. You can run multiple instances,
subject to:

- the outbox claim query;
- RabbitMQ listener concurrency and prefetch settings;
- STOMP relay mode for cross-instance WebSocket delivery;
- shared database connection limits;
- scheduler execution on each instance (the relay is safe, but other scheduled jobs may not be).

If you add scheduled jobs, guard them with a distributed lock or run them in a dedicated worker.

## Migrations and rollout

A safe sequence for a backward-compatible change:

1. Deploy schema additions.
2. Deploy application code that writes both old and new representations.
3. Backfill.
4. Switch reads.
5. Remove old fields in a later release.

Never assume a rollback will reverse a destructive migration.

## Graceful shutdown

`server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s` are enabled. Ensure
your platform sends SIGTERM and allows the termination grace period to exceed the shutdown timeout.

## Known operational limits

- The included relay is a straightforward example, not a general-purpose high-throughput outbox.
- The starter uses HTTP Basic for demonstration; production identity is deployment-specific.
- The simple STOMP broker is single-instance only; use relay mode to scale WebSockets.
- Database schema evolution remains a human-reviewed operational process.
