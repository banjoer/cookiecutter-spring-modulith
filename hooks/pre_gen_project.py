"""Validate Cookiecutter input before anything is written to disk."""

from __future__ import annotations

import keyword
import re
import sys

PROJECT_SLUG_RE = re.compile(r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$")
PACKAGE_SEGMENT_RE = re.compile(r"^[a-z][a-z0-9_]*$")
JAVA_KEYWORDS = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new",
    "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while", "true", "false", "null",
    "record", "sealed", "permits", "non-sealed", "var", "yield", "module",
    "open", "opens", "requires", "exports", "provides", "uses", "to", "with",
    "transitive", "_",
}

PROJECT_NAME = "{{ cookiecutter.project_name }}".strip()
PROJECT_SLUG = "{{ cookiecutter.project_slug }}".strip()
DESCRIPTION = "{{ cookiecutter.project_description }}".strip()
PACKAGE_NAME = "{{ cookiecutter.package_name }}".strip()
JAVA_VERSION = "{{ cookiecutter.java_version }}".strip()
SPRING_BOOT_VERSION = "{{ cookiecutter.spring_boot_version }}".strip()
BUILD_TOOL = "{{ cookiecutter.build_tool }}".strip()
DATABASE = "{{ cookiecutter.database }}".strip()
MIGRATION_TOOL = "{{ cookiecutter.migration_tool }}".strip()
BOOLEAN_OPTIONS = {
    "include_identity_module": "{{ cookiecutter.include_identity_module }}".strip().lower(),
    "include_notifications_module": "{{ cookiecutter.include_notifications_module }}".strip().lower(),
    "include_audit_module": "{{ cookiecutter.include_audit_module }}".strip().lower(),
    "include_rabbitmq": "{{ cookiecutter.include_rabbitmq }}".strip().lower(),
    "include_stomp": "{{ cookiecutter.include_stomp }}".strip().lower(),
    "include_docker_compose": "{{ cookiecutter.include_docker_compose }}".strip().lower(),
    "include_testcontainers": "{{ cookiecutter.include_testcontainers }}".strip().lower(),
    "include_observability": "{{ cookiecutter.include_observability }}".strip().lower(),
}

SPRING_MODULITH_BY_BOOT = {
    "4.2": "2.2.0",
    "4.1": "2.1.3",
    "4.0": "2.0.10",
}


def fail(message: str) -> None:
    print(f"\nERROR: {message}\n", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if not PROJECT_NAME or len(PROJECT_NAME) > 100:
        fail("project_name must be between 1 and 100 characters long.")
    if any(character in PROJECT_NAME for character in ('"', "\\", "\n", "\r")):
        fail("project_name must not contain quotes, backslashes, or line breaks.")
    if not DESCRIPTION or len(DESCRIPTION) > 300:
        fail("project_description must be between 1 and 300 characters long.")
    if any(character in DESCRIPTION for character in ('"', "\\", "\n", "\r")):
        fail("project_description must not contain quotes, backslashes, or line breaks.")

    if not PROJECT_SLUG_RE.match(PROJECT_SLUG):
        fail(
            "project_slug must be lower-kebab-case and start with a letter, "
            f"for example 'my-service'. Received: {PROJECT_SLUG!r}"
        )
    if PROJECT_SLUG in {"build", "src", "gradle", "test", "main", "app"}:
        fail(f"project_slug {PROJECT_SLUG!r} is reserved. Choose another name.")

    package_segments = PACKAGE_NAME.split(".")
    if len(package_segments) < 2:
        fail("package_name must contain at least two segments, for example 'com.example.application'.")
    for segment in package_segments:
        if not PACKAGE_SEGMENT_RE.match(segment):
            fail(
                f"Invalid Java package segment {segment!r}. Segments must be lowercase, "
                "start with a letter, and contain only letters, digits, or underscores."
            )
        if segment in JAVA_KEYWORDS or keyword.iskeyword(segment):
            fail(f"Java package segment {segment!r} is a reserved keyword.")

    if JAVA_VERSION not in {"21", "25"}:
        fail(f"java_version must be 21 or 25. Received: {JAVA_VERSION!r}")
    if BUILD_TOOL != "gradle-kotlin":
        fail(f"build_tool must be 'gradle-kotlin'. Received: {BUILD_TOOL!r}")
    if DATABASE != "postgresql":
        fail(f"database must be 'postgresql'. Received: {DATABASE!r}")
    if MIGRATION_TOOL != "flyway":
        fail(f"migration_tool must be 'flyway'. Received: {MIGRATION_TOOL!r}")

    boot_prefix = ".".join(SPRING_BOOT_VERSION.split(".")[:2])
    if boot_prefix not in SPRING_MODULITH_BY_BOOT:
        fail(
            "spring_boot_version must be a supported Spring Boot 4.x release. "
            f"Supported lines: {', '.join(sorted(SPRING_MODULITH_BY_BOOT))}."
        )
    if SPRING_BOOT_VERSION not in {"4.2.0", "4.1.3"}:
        print(
            f"WARNING: Spring Boot {SPRING_BOOT_VERSION} is not one of the tested template versions. "
            f"Spring Modulith {SPRING_MODULITH_BY_BOOT[boot_prefix]} will be selected.",
            file=sys.stderr,
        )

    for option, value in BOOLEAN_OPTIONS.items():
        if value not in {"true", "false"}:
            fail(f"{option} must be true or false. Received: {value!r}")


if __name__ == "__main__":
    main()
