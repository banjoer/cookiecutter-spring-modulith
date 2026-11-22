plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "com.example.springmodulithapplication"
version = "0.0.1-SNAPSHOT"
description = "A modular monolith built with Spring Boot and Spring Modulith."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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



    implementation("org.springframework.boot:spring-boot-starter-rabbitmq")



    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // Required by Spring's STOMP broker relay TCP client.
    implementation("io.projectreactor.netty:reactor-netty-core")


    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.modulith:spring-modulith-observability")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.awaitility:awaitility")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    testImplementation("org.springframework.boot:spring-boot-starter-rabbitmq-test")



    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")


    testImplementation("org.testcontainers:testcontainers-postgresql")

    testImplementation("org.testcontainers:testcontainers-rabbitmq")


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
