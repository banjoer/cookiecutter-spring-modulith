"""Feature-specific pruning checks for adapters that require their dependencies."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[1]


def generate(target: Path, *arguments: str) -> None:
    result = subprocess.run(
        [sys.executable, "-m", "cookiecutter", "--no-input", str(REPOSITORY), *arguments],
        cwd=target,
        check=False,
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, result.stderr + result.stdout


def test_rabbit_only_removes_stomp_sources_and_test(tmp_path: Path) -> None:
    generate(
        tmp_path,
        "project_slug=rabbit-only",
        "package_name=com.example.rabbitonfly",
        "include_stomp=false",
    )
    package = tmp_path / "rabbit-only/src/main/java/com/example/rabbitonfly"
    tests = tmp_path / "rabbit-only/src/test/java/com/example/rabbitonfly"

    assert (package / "messaging/rabbitmq/RabbitMQConfiguration.java").is_file()
    assert not (package / "messaging/websocket").exists()
    assert not (tests / "integration/StompIntegrationTest.java").exists()
    assert (tests / "messaging/rabbitmq/RabbitMQIntegrationTest.java").is_file()
    assert not (tmp_path / "rabbit-only/docs/stomp.md").exists()


def test_stomp_only_removes_rabbit_sources_and_tests(tmp_path: Path) -> None:
    generate(
        tmp_path,
        "project_slug=stomp-only",
        "package_name=com.example.stomponly",
        "include_rabbitmq=false",
    )
    package = tmp_path / "stomp-only/src/main/java/com/example/stomponly"
    tests = tmp_path / "stomp-only/src/test/java/com/example/stomponly"

    assert (package / "messaging/websocket/WebSocketConfiguration.java").is_file()
    assert not (package / "messaging/rabbitmq").exists()
    assert not (tests / "messaging/rabbitmq").exists()
    assert (tests / "integration/StompIntegrationTest.java").is_file()
    assert not (tmp_path / "stomp-only/docs/rabbitmq.md").exists()
