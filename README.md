# cookiecutter-spring-modulith

A production-oriented [Cookiecutter](https://cookiecutter.readthedocs.io/) template for
**single-deployable modular monoliths** built with Java, Spring Boot 4, and Spring Modulith.

Generate a real Gradle project — not a starter kit of snippets — with explicit module boundaries,
runtime-switchable optional modules, PostgreSQL/Flyway, RabbitMQ, STOMP, security, observability,
Docker Compose, and a Docker-backed test suite.

## What you get

- **One Gradle project, one Spring Boot JAR.** No multi-project build and no separate services.
- **Spring Modulith architecture enforcement** with `@ApplicationModule`, named interfaces, and
  `ApplicationModules.of(...).verify()`.
- **Hybrid module registration:** core modules are imported explicitly; optional modules use Spring
  Boot auto-configuration; infrastructure adapters use conditional configuration.
- **Runtime activation** through `app.modules.*.enabled` and `app.integrations.*.enabled`.
  Optional module source stays compiled, so the same artifact can be promoted with different
  feature flags.
- **Typed, validated configuration** per module and adapter, with environment-variable overrides.
- **Identity example module** with aggregate, repository, service, public API, events, REST,
  BCrypt security, and tests.
- **Notifications example** with optional email, async events, persistence, and enabled/disabled
  tests.
- **Audit example** with event-driven processing, isolated persistence, and integration tests.
- **RabbitMQ AMQP** with JSON, publisher confirms, transactional outbox, idempotent inbox, retry,
  dead-letter queue, correlation IDs, and Testcontainers coverage.
- **STOMP/WebSocket** with configurable endpoint/prefixes, authenticated CONNECT, and selectable
  in-memory or RabbitMQ relay broker strategy.
- **PostgreSQL + Flyway** with module-owned tables, per-module migration folders, and global
  version ranges.
- **Docker Compose** for PostgreSQL, RabbitMQ with the STOMP plugin, and the generated application.
- **Security, health/readiness, correlation IDs, structured production logging, Prometheus and
  OpenTelemetry hooks.**
- **Tests:** unit, architecture, activation, PostgreSQL Testcontainers, RabbitMQ Testcontainers,
  security, and STOMP.

## Requirements

- Python 3.9+ and Cookiecutter 2.x
- Java 21+ (the generated Gradle build uses a toolchain; it can provision the selected JDK)
- Docker with Compose v2 for the generated integration tests and local infrastructure

Install Cookiecutter:

```bash
python3 -m pip install --user cookiecutter
# or: pipx install cookiecutter
```

## Generate a project

From this repository:

```bash
cookiecutter .
```

Or from a checkout of this repository:

```bash
cookiecutter /path/to/cookiecutter-spring-modulith
```

Non-interactive example:

```bash
cookiecutter --no-input . \
  project_name="Orders Platform" \
  project_slug=orders-platform \
  package_name=com.acme.orders
```

The generated project is created in `./orders-platform`.

### Options

| Option | Choices / default | Meaning |
| --- | --- | --- |
| `project_name` | `Spring Modulith Application` | Human-readable name |
| `project_slug` | derived from name | Directory and image name; lower-kebab-case |
| `project_description` | example text | Short description |
| `package_name` | `com.example.<slug>` | Root Java package |
| `java_version` | `21`, `25` | Gradle toolchain and Docker base image |
| `spring_boot_version` | `4.2.0`, `4.1.3` | Boot line; Spring Modulith is selected automatically |
| `build_tool` | `gradle-kotlin` | Gradle Kotlin DSL is the supported build |
| `database` | `postgresql` | PostgreSQL is the supported database |
| `migration_tool` | `flyway` | Flyway is the supported migration tool |
| `include_identity_module` | `true` | Enable identity by default at runtime |
| `include_notifications_module` | `true` | Enable notifications by default at runtime |
| `include_audit_module` | `true` | Enable audit by default at runtime |
| `include_rabbitmq` | `true` | Generate the AMQP adapter and broker dependency |
| `include_stomp` | `true` | Generate the STOMP adapter and WebSocket dependency |
| `include_docker_compose` | `true` | Generate `docker-compose.yml` |
| `include_testcontainers` | `true` | Generate Docker-backed integration tests and dependencies |
| `include_observability` | `true` | Generate OpenTelemetry export and observability dependencies |

### Important: inclusion vs. activation

Cookiecutter options decide which **capabilities and adapters are generated** (for example, whether
the RabbitMQ adapter source and dependency exist). Spring configuration decides which included
capabilities are **active at runtime**.

For the optional business modules (identity, notifications, audit), the mandatory architecture keeps
the source compiled in every generated project. The `include_*_module` flags set the **initial
runtime default** in `application.yml`. This is intentional: an operator can disable a capability
without regenerating or rebuilding a different artifact. Use `app.modules.<module>.enabled` to
toggle it later.

### Validation

`hooks/pre_gen_project.py` rejects invalid slugs, package names, Java keywords in package segments,
unsupported versions, and malformed booleans. `hooks/post_gen_project.py` moves the Java package
into the correct directory structure and removes adapters that were not selected.

## Version compatibility

| Spring Boot | Spring Modulith |
| --- | --- |
| 4.2.x | 2.2.x |
| 4.1.x | 2.1.x |
| 4.0.x | 2.0.x |

The template defaults to Spring Boot **4.2.0** and Spring Modulith **2.2.0**, with Java 21. It uses
Boot's BOM for Spring, Jackson, Flyway, Testcontainers, Micrometer, and OpenTelemetry versions. Only
stable releases are used.

## Generated structure

```text
<project-slug>/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradlew, gradlew.bat, gradle/wrapper/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── README.md
├── docs/
└── src/
    ├── main/
    │   ├── java/<package>/
    │   │   ├── Application.java
    │   │   ├── bootstrap/
    │   │   ├── identity/
    │   │   ├── notifications/
    │   │   ├── audit/
    │   │   ├── messaging/
    │   │   ├── infrastructure/
    │   │   └── shared/
    │   └── resources/
    │       ├── application*.yml
    │       ├── META-INF/spring/...AutoConfiguration.imports
    │       └── db/migration/
    └── test/java/<package>/
        ├── architecture/
        ├── identity/
        ├── integration/
        ├── messaging/
        └── support/
```

The generated README covers local development and every configuration option. The generated `docs/`
cover architecture, adding modules, events, migrations, RabbitMQ, STOMP, testing, production, and
conventions.

## Validate the template

Generate the default project and run the real build:

```bash
rm -rf /tmp/smj-check
mkdir -p /tmp/smj-check
cd /tmp/smj-check
cookiecutter --no-input /path/to/cookiecutter-spring-modulith
cd spring-modulith-application
./gradlew test
```

The test suite requires Docker for Testcontainers. Use
`./gradlew test -PskipIntegrationTests` for a fast compile/unit/architecture check.

## Template development

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements-dev.txt
pytest -q tests
```

The tests generate the default and several feature-reduced projects and verify that optional
adapter sources are pruned correctly. The GitHub Actions workflow generates the default project and
runs its full Gradle + Testcontainers suite.

## Design notes

### Why not component-scan the root package?

`@SpringBootApplication` is required by Spring Modulith's runtime, but its scan is narrowed to the
`bootstrap` package. Each module scans only its own internals inside its activation boundary. This
is what makes disabling a module actually unregister its beans.

### Why are modules not separate Gradle projects?

The requirement is a modular monolith, not a distributed system. Separate Gradle modules make it
easy to accidentally create a distributed deployment and add build complexity without enforcing
runtime boundaries. Spring Modulith plus architecture tests enforce the boundaries that matter.

### Why an outbox instead of just `RabbitTemplate.convertAndSend`?

Domain events are persisted locally; delivery to an external broker needs a different contract.
The included outbox writes messages in the listener transaction, relays with confirms, and marks
only acknowledged messages as published. The consumer uses an inbox for idempotency. See the
generated RabbitMQ documentation.

### Why is STOMP separate from RabbitMQ?

AMQP and STOMP are distinct protocols and independent capabilities. Disabling one must not disable
the other. A RabbitMQ STOMP relay also requires the `rabbitmq_stomp` plugin, which is separate from
AMQP. The generated infrastructure treats them as independent conditional adapters.

## Known limitations

- HTTP Basic is a working security foundation, not a production identity provider. Integrate OIDC,
  token authentication, or mTLS before production.
- The simple STOMP broker is in-memory and single-instance. Use relay mode for scale.
- The included outbox relay is intentionally compact. For very high throughput or complex retention,
  use a dedicated outbox library.
- Testcontainers requires Docker. The fast test mode skips Docker-backed tests.
- The template supports Gradle Kotlin DSL, PostgreSQL, and Flyway by design; unsupported
  combinations are rejected rather than generated untested.
- The default project is validated end-to-end with Java 21 and Spring Boot 4.2.0. The Spring Boot
  4.1.3 combination also passes the full test suite. Java 25 compilation is validated; the full
  Docker-backed suite is exercised on Java 21.
- The sample in `examples/default` is a generated snapshot. Regenerate it after changing the
  template so it does not drift.

## Validation results

The checked-in example and the template variants were exercised with the actual generated output:

| Scenario | Result |
| --- | --- |
| Default example (`examples/default`), `./gradlew test` | 15 tests passed |
| Spring Boot 4.1.3 generated project, `./gradlew test` | 15 tests passed |
| Minimal project (all optional modules/adapters disabled, no Testcontainers) | 3 tests passed |
| RabbitMQ-only and STOMP-only generated projects | `compileTestJava` passed |
| `docker compose up -d --build` | PostgreSQL, RabbitMQ, and app healthy; registration, notifications, audit, outbox/inbox verified |
| STOMP relay mode against RabbitMQ `rabbitmq_stomp` | Authenticated CONNECT, subscribe, send, and receive verified |
| Java 25 generated project | `compileJava` passed |

The default example's tests include PostgreSQL and RabbitMQ Testcontainers. Docker is required for
the full suite; `-PskipIntegrationTests` provides a fast Docker-free unit/architecture check.

## Generated example

A validated default output is checked in at [`examples/default`](examples/default). It is a normal
Gradle project with its own README and documentation. Regenerate it with:

```bash
rm -rf examples/default
mkdir -p examples/.tmp
cd examples/.tmp
cookiecutter --no-input ../..
mv spring-modulith-application ../default
cd ../..
rmdir examples/.tmp
```

The example is generated with default options: all modules enabled, RabbitMQ, STOMP, Compose,
Testcontainers, and observability included.

## Repository layout

```text
cookiecutter-spring-modulith/
├── cookiecutter.json
├── hooks/
│   ├── pre_gen_project.py
│   └── post_gen_project.py
├── tests/
├── README.md
├── LICENSE
├── .gitignore
├── examples/default/
└── {{cookiecutter.project_slug}}/
```

## License

MIT. See [LICENSE](LICENSE).
