package com.example.springmodulithapplication.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.springmodulithapplication.identity.IdentityModuleConfiguration;
import com.example.springmodulithapplication.infrastructure.InfrastructureConfiguration;
import com.example.springmodulithapplication.messaging.MessagingConfiguration;

/**
 * Explicit registration of the core modules and the infrastructure composition root.
 *
 * <p>Only genuinely core modules belong here. Optional modules (notifications and audit) register
 * themselves through Spring Boot auto-configuration so that this class does not need to change
 * when an optional capability is toggled.</p>
 */
@Configuration(proxyBeanMethods = false)
@Import({
        IdentityModuleConfiguration.class,
        InfrastructureConfiguration.class,
        MessagingConfiguration.class
})
public class ApplicationModulesConfiguration {
}
