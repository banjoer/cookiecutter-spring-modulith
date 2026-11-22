package com.example.springmodulithapplication.architecture;

import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModules;

import com.example.springmodulithapplication.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails the build if a module reaches into another module's internals or introduces a cycle.
 */
class ApplicationModulesTest {

    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void discoversTheExpectedModules() {
        assertThat(modules.stream().map(module -> module.getBasePackage().getName()))
                .contains(
                        "com.example.springmodulithapplication.identity",
                        "com.example.springmodulithapplication.notifications",
                        "com.example.springmodulithapplication.audit",
                        "com.example.springmodulithapplication.messaging",
                        "com.example.springmodulithapplication.infrastructure",
                        "com.example.springmodulithapplication.bootstrap");
    }
}
