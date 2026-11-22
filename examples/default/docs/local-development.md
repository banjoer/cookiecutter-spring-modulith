# Local development and Docker Compose

## Prerequisites

- Java 21. The Gradle toolchain can provision it if your machine does not
  have it.
- Docker with Compose v2.
- PostgreSQL.
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

```bash
docker compose up -d postgres rabbitmq
docker compose ps
```

PostgreSQL is available on `localhost:5432`. RabbitMQ AMQP is on `5672`, management on `15672`,
and STOMP on `61613`. The RabbitMQ container enables the `rabbitmq_stomp` plugin explicitly.

Health checks gate the application container on database and broker readiness.

If you change `POSTGRES_PASSWORD` after the database volume has been initialized, the old
credential remains in the volume. Reset local data with `docker compose down -v` before starting
again.


## Run with Gradle

```bash
set -a; source .env; set +a
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The `dev` profile enables SQL formatting and debug logging for the application package. It does not
weaken security or provide default passwords.

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


## Profiles

| Profile | Purpose |
| --- | --- |
| default / `dev` | Local development |
| `test` | Testcontainers-backed tests; disables external export and broker adapters by default |
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
| `GET /api/identity/me` | Authenticated |
| `GET /api/notifications` | Authenticated |
| `GET /api/audit` | Authenticated |

| `ws://localhost:8080/ws` | STOMP CONNECT authentication |


## Debugging

- Add `logging.level.com.example.springmodulithapplication=DEBUG` to see application behavior.
- Correlation IDs appear in dev logs and the `X-Correlation-ID` response header.
- Use `spring.jpa.show-sql=true` only on a local branch; the dev profile formats SQL without
  enabling open-session-in-view.
- RabbitMQ management UI: <http://localhost:15672>.
- If a port is already in use, change `APP_PORT` or the compose port mappings.

## Tests

```bash
./gradlew test
```

Testcontainers pulls PostgreSQL and RabbitMQ images. The first run can take longer.
Use `-PskipIntegrationTests` for a fast, Docker-free feedback loop.

