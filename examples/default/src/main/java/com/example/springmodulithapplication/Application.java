package com.example.springmodulithapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;

import com.example.springmodulithapplication.bootstrap.ApplicationModulesConfiguration;

/**
 * Single deployable entry point for the modular monolith.
 *
 * <p>{@link SpringBootApplication} is required by Spring Modulith's runtime integration. Its
 * component scan is deliberately narrowed to the bootstrap package so that disabling a module
 * actually prevents its services, controllers, and listeners from being registered. Business
 * modules add their own scoped scans inside their activation boundaries.</p>
 */
@SpringBootApplication(scanBasePackages = "com.example.springmodulithapplication.bootstrap")
@EnableAsync
@Modulithic(
        systemName = "Spring Modulith Application",
        sharedModules = "shared")
@Import(ApplicationModulesConfiguration.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
