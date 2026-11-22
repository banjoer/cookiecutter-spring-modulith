# Local development{% if cookiecutter.include_docker_compose|string|lower == 'true' %} and Docker Compose{% endif %}

## Prerequisites

- Java {{ cookiecutter.java_version }}. The Gradle toolchain can provision it if your machine does not
  have it.
{% if cookiecutter.include_docker_compose|string|lower == 'true' or cookiecutter.include_testcontainers|string|lower == 'true' -%}
- Docker with Compose v2.
{% endif -%}
- PostgreSQL{% if cookiecutter.include_docker_compose|string|lower != 'true' %} available locally or through your platform{% endif %}.
- The Gradle wrapper is included; no local Gradle installation is required.

## Environment file

```bash
cp .env.example .env
```

Edit the passwords in `.env`. The example values are intentionally local-only. Never commit `.env`.

For `bootRun`, export the environment first:

```bash
set -a
source .env
set +a
```

## Infrastructure

{% if cookiecutter.include_docker_compose|string|lower == 'true' -%}
```bash
docker compose up -d postgres{% if cookiecutter.include_rabbitmq|string|lower == 'true' %} rabbitmq{% endif %}
docker compose ps
```

PostgreSQL is available on `localhost:5432`. {% if cookiecutter.include_rabbitmq|string|lower == 'true' %}RabbitMQ AMQP is on `5672`, management on `15672`,
and STOMP on `61613`. The RabbitMQ container enables the `rabbitmq_stomp` plugin explicitly.{% endif %}

Health checks gate the application container on database{% if cookiecutter.include_rabbitmq|string|lower == 'true' %} and broker{% endif %} readiness.

If you change `POSTGRES_PASSWORD` after the database volume has been initialized, the old
credential remains in the volume. Reset local data with `docker compose down -v` before starting
again.
{% else -%}
This project was generated without Compose. Start PostgreSQL{% if cookiecutter.include_rabbitmq|string|lower == 'true' %} and RabbitMQ{% endif %} using your local tooling or platform,
then export the variables from `.env.example`.
{% endif %}

## Run with Gradle

```bash
set -a; source .env; set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The `dev` profile enables SQL formatting and debug logging for the application package. It does not
weaken security or provide default passwords.

{% if cookiecutter.include_docker_compose|string|lower == 'true' -%}
## Run the full stack

```bash
docker compose up -d --build
docker compose logs -f app
```

The application image is built from the included multi-stage `Dockerfile`. It runs as a non-root
user and exposes a container health check against `/actuator/health/readiness`.

To stop and remove local data:

```bash
docker compose down -v
```
{% else -%}
## Container image

The included multi-stage `Dockerfile` builds a non-root application image with a readiness health
check. Build it with:

```bash
docker build -t {{ cookiecutter.project_slug }}:local .
```
{% endif %}

## Profiles

| Profile | Purpose |
| --- | --- |
| default / `dev` | Local development |
| `test` | {% if cookiecutter.include_testcontainers|string|lower == 'true' %}Testcontainers-backed{% else %}Fast unit and architecture{% endif %} tests; disables external export and broker adapters by default |
| `prod` | Structured JSON logs, stricter health details, production-oriented pool defaults |

Production behavior is configured through environment variables, not by rebuilding with a different
profile-specific source tree.

## Useful endpoints

| Endpoint | Authentication |
| --- | --- |
| `GET /actuator/health` | Public |
| `GET /actuator/health/readiness` | Public |
| `GET /actuator/health/liveness` | Public |
| `GET /actuator/info` | Public |
| `GET /actuator/prometheus` | Authenticated |
| `POST /api/identity/users` | Public registration |
{% if cookiecutter.include_identity_module|string|lower == 'true' -%}
| `GET /api/identity/me` | Authenticated |
{% endif -%}
{% if cookiecutter.include_notifications_module|string|lower == 'true' -%}
| `GET /api/notifications` | Authenticated |
{% endif -%}
{% if cookiecutter.include_audit_module|string|lower == 'true' -%}
| `GET /api/audit` | Authenticated |
{% endif -%}
{% if cookiecutter.include_stomp|string|lower == 'true' %}
| `ws://localhost:8080/ws` | STOMP CONNECT authentication |
{% endif %}

## Debugging

- Add `logging.level.{{ cookiecutter.package_name }}=DEBUG` to see application behavior.
- Correlation IDs appear in dev logs and the `X-Correlation-ID` response header.
- Use `spring.jpa.show-sql=true` only on a local branch; the dev profile formats SQL without
  enabling open-session-in-view.
- RabbitMQ management UI: <http://localhost:15672>.
- If a port is already in use, change `APP_PORT` or the compose port mappings.

## Tests

{% if cookiecutter.include_testcontainers|string|lower == 'true' -%}
```bash
./gradlew test
```

Testcontainers pulls PostgreSQL{% if cookiecutter.include_rabbitmq|string|lower == 'true' %} and RabbitMQ{% endif %} images. The first run can take longer.
Use `-PskipIntegrationTests` for a fast, Docker-free feedback loop.
{% else -%}
```bash
./gradlew test
```

This project was generated without Testcontainers, so the included suite is limited to unit and
architecture tests and does not start PostgreSQL{% if cookiecutter.include_rabbitmq|string|lower == 'true' %} or RabbitMQ{% endif %}.
{% endif %}
