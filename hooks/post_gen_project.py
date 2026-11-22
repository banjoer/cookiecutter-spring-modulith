"""Finish the generated project: move Java packages and prune excluded adapters."""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

PACKAGE_NAME = "{{ cookiecutter.package_name }}".strip()
PROJECT_SLUG = "{{ cookiecutter.project_slug }}".strip()
JAVA_VERSION = "{{ cookiecutter.java_version }}".strip()
SPRING_BOOT_VERSION = "{{ cookiecutter.spring_boot_version }}".strip()

INCLUDE_IDENTITY = "{{ cookiecutter.include_identity_module }}".strip().lower() == "true"
INCLUDE_NOTIFICATIONS = "{{ cookiecutter.include_notifications_module }}".strip().lower() == "true"
INCLUDE_AUDIT = "{{ cookiecutter.include_audit_module }}".strip().lower() == "true"
INCLUDE_RABBITMQ = "{{ cookiecutter.include_rabbitmq }}".strip().lower() == "true"
INCLUDE_STOMP = "{{ cookiecutter.include_stomp }}".strip().lower() == "true"
INCLUDE_DOCKER_COMPOSE = "{{ cookiecutter.include_docker_compose }}".strip().lower() == "true"
INCLUDE_TESTCONTAINERS = "{{ cookiecutter.include_testcontainers }}".strip().lower() == "true"
INCLUDE_OBSERVABILITY = "{{ cookiecutter.include_observability }}".strip().lower() == "true"


def move_java_package(root: Path) -> None:
    """Cookiecutter stores ``com.example.app`` as one directory; Java needs nesting."""
    parts = PACKAGE_NAME.split(".")
    source = root / "src" / "main" / "java" / PACKAGE_NAME
    target = root / "src" / "main" / "java" / Path(*parts)
    if source.exists() and source != target:
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            shutil.rmtree(target)
        shutil.move(str(source), str(target))

    test_source = root / "src" / "test" / "java" / PACKAGE_NAME
    test_target = root / "src" / "test" / "java" / Path(*parts)
    if test_source.exists() and test_source != test_target:
        test_target.parent.mkdir(parents=True, exist_ok=True)
        if test_target.exists():
            shutil.rmtree(test_target)
        shutil.move(str(test_source), str(test_target))


def remove(path: Path) -> None:
    if path.is_dir():
        shutil.rmtree(path)
    elif path.exists():
        path.unlink()


def main() -> None:
    root = Path.cwd()
    move_java_package(root)

    package_root = root / "src/main/java" / Path(*PACKAGE_NAME.split("."))
    test_root = root / "src/test/java" / Path(*PACKAGE_NAME.split("."))

    if not INCLUDE_RABBITMQ:
        remove(package_root / "messaging" / "rabbitmq")
        remove(test_root / "messaging" / "rabbitmq")
        remove(test_root / "support" / "AbstractMessagingIntegrationTest.java")
        remove(root / "src/main/resources/db/migration/messaging/V400__messaging_create_outbox_and_inbox.sql")
        remove(root / "docs" / "rabbitmq.md")

    if not INCLUDE_STOMP:
        remove(package_root / "messaging" / "websocket")
        remove(test_root / "messaging" / "websocket")
        remove(test_root / "integration" / "StompIntegrationTest.java")
        remove(root / "docs" / "stomp.md")

    if not INCLUDE_TESTCONTAINERS:
        remove(test_root / "support")
        remove(test_root / "integration")
        remove(test_root / "messaging")

    if not INCLUDE_DOCKER_COMPOSE:
        remove(root / "docker-compose.yml")

    gradlew = root / "gradlew"
    if gradlew.exists():
        gradlew.chmod(gradlew.stat().st_mode | 0o111)

    modules = [
        name
        for name, included in (
            ("identity", INCLUDE_IDENTITY),
            ("notifications", INCLUDE_NOTIFICATIONS),
            ("audit", INCLUDE_AUDIT),
        )
        if included
    ]
    integrations = []
    if INCLUDE_RABBITMQ:
        integrations.append("RabbitMQ AMQP")
    if INCLUDE_STOMP:
        integrations.append("STOMP/WebSocket")
    if INCLUDE_OBSERVABILITY:
        integrations.append("OpenTelemetry")
    if INCLUDE_DOCKER_COMPOSE:
        integrations.append("Docker Compose")

    print()
    print(f"Generated {PROJECT_SLUG} (Java {JAVA_VERSION}, Spring Boot {SPRING_BOOT_VERSION}).")
    print(f"Enabled by default: modules={modules or ['none']}; integrations={integrations or ['none']}.")
    print("Optional module code is always present and is toggled with app.modules.*.enabled.")
    print()
    print("Next steps:")
    print(f"  cd {PROJECT_SLUG}")
    if INCLUDE_DOCKER_COMPOSE:
        print("  cp .env.example .env    # set local passwords")
        print("  docker compose up -d postgres"
              + (" rabbitmq" if INCLUDE_RABBITMQ else ""))
    print("  ./gradlew test")
    print("  ./gradlew bootRun --args='--spring.profiles.active=dev'")
    print()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001 - surface a useful message to Cookiecutter users
        print(f"\nERROR while finishing the generated project: {error}\n", file=sys.stderr)
        raise
