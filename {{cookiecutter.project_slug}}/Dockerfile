# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:{{ cookiecutter.java_version }}-jdk AS build
WORKDIR /workspace

# Warm the Gradle and dependency caches before copying application sources.
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x gradlew \
    && ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:{{ cookiecutter.java_version }}-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --create-home app

WORKDIR /app
COPY --from=build /workspace/build/libs/application.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
