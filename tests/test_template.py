"""Smoke tests for the Cookiecutter template itself."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[1]


def run_cookiecutter(target: Path, *extra_args: str, expect_success: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        [sys.executable, "-m", "cookiecutter", "--no-input", str(REPOSITORY), *extra_args],
        cwd=target,
        check=False,
        capture_output=True,
        text=True,
    )
    if expect_success:
        assert result.returncode == 0, result.stderr + result.stdout
    return result


def test_generates_default_project(tmp_path: Path) -> None:
    run_cookiecutter(tmp_path)

    project = tmp_path / "spring-modulith-application"
    assert (project / "build.gradle.kts").is_file()
    assert (project / "docker-compose.yml").is_file()
    assert (project / "gradlew").is_file()
    assert (project / ".env.example").is_file()

    package = project / "src/main/java/com/example/springmodulithapplication"
    assert (package / "Application.java").is_file()
    assert (package / "identity/IdentityModuleConfiguration.java").is_file()
    assert (package / "notifications/NotificationsAutoConfiguration.java").is_file()
    assert (package / "audit/AuditAutoConfiguration.java").is_file()
    assert (package / "messaging/rabbitmq/RabbitMQConfiguration.java").is_file()
    assert (package / "messaging/websocket/WebSocketConfiguration.java").is_file()
    assert (project / "src/test/java/com/example/springmodulithapplication/integration").is_dir()

    rendered_files = [
        path
        for path in project.rglob("*")
        if path.is_file() and path.suffix in {".java", ".kts", ".yml", ".md", ".py", ".sql", ".toml"}
    ]
    assert rendered_files
    for path in rendered_files:
        if path.name in {"kickoff.prompt.md"}:
            continue
        text = path.read_text(encoding="utf-8")
        assert "{%" not in text, f"unrendered Jinja block in {path}"
        assert "{{" not in text, f"unrendered Jinja expression in {path}"


def test_minimal_project_removes_unselected_adapters(tmp_path: Path) -> None:
    run_cookiecutter(
        tmp_path,
        "project_name=Minimal App",
        "project_slug=minimal-app",
        "package_name=com.example.minimal",
        "include_identity_module=false",
        "include_notifications_module=false",
        "include_audit_module=false",
        "include_rabbitmq=false",
        "include_stomp=false",
        "include_docker_compose=false",
        "include_testcontainers=false",
        "include_observability=false",
    )

    project = tmp_path / "minimal-app"
    package = project / "src/main/java/com/example/minimal"
    assert (package / "Application.java").is_file()
    assert not (package / "messaging/rabbitmq").exists()
    assert not (package / "messaging/websocket").exists()
    assert not (project / "docker-compose.yml").exists()
    assert not (project / "src/test/java/com/example/minimal/integration").exists()
    assert (package / "identity/IdentityModuleConfiguration.java").is_file()
    assert (project / "src/main/resources/db/migration/infrastructure/V900__infrastructure_event_publication.sql").is_file()
    assert not (project / "src/main/resources/db/migration/messaging/V400__messaging_create_outbox_and_inbox.sql").exists()


def test_rejects_invalid_package_name(tmp_path: Path) -> None:
    result = run_cookiecutter(
        tmp_path,
        "package_name=com.example.Class",
        expect_success=False,
    )
    assert result.returncode != 0
    assert "ERROR" in result.stderr + result.stdout
