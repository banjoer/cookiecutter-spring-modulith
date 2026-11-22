{% set rabbit = cookiecutter.include_rabbitmq|string|lower == 'true' -%}
{% set stomp = cookiecutter.include_stomp|string|lower == 'true' -%}
{% set observability = cookiecutter.include_observability|string|lower == 'true' -%}
{% set testcontainers = cookiecutter.include_testcontainers|string|lower == 'true' -%}
plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "{{ cookiecutter.package_name }}"
version = "0.0.1-SNAPSHOT"
description = "{{ cookiecutter.project_description }}"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of({{ cookiecutter.java_version }})
    }
}

repositories {
    mavenCentral()
}

val springBootVersion = libs.versions.spring.boot.get()
val springModulithVersion = libs.versions.spring.modulith.get()

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(platform("org.springframework.modulith:spring-modulith-bom:$springModulithVersion"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.springframework.modulith:spring-modulith-bom:$springModulithVersion"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    implementation("org.springframework.modulith:spring-modulith-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

{% if rabbit %}
{% if cookiecutter.spring_boot_version.startswith('4.2') %}
    implementation("org.springframework.boot:spring-boot-starter-rabbitmq")
{% else %}
    implementation("org.springframework.boot:spring-boot-starter-amqp")
{% endif %}
{% endif %}
{% if stomp %}
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // Required by Spring's STOMP broker relay TCP client.
    implementation("io.projectreactor.netty:reactor-netty-core")
{% endif %}
{% if observability %}
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.modulith:spring-modulith-observability")
{% endif %}

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.awaitility:awaitility")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
{% if rabbit %}
{% if cookiecutter.spring_boot_version.startswith('4.2') %}
    testImplementation("org.springframework.boot:spring-boot-starter-rabbitmq-test")
{% else %}
    testImplementation("org.springframework.boot:spring-boot-starter-amqp-test")
{% endif %}
{% endif %}
{% if stomp %}
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
{% endif %}
{% if testcontainers %}
    testImplementation("org.testcontainers:testcontainers-postgresql")
{% if rabbit %}
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
{% endif %}
{% endif %}
}

springBoot {
    buildInfo()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (project.hasProperty("skipIntegrationTests")) {
            excludeTags("integration")
        }
    }
    systemProperty("spring.profiles.active", "test")
    maxParallelForks = 1
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "application.jar"
}
