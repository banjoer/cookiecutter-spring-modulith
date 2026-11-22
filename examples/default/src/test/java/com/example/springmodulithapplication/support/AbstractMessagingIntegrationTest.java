package com.example.springmodulithapplication.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Adds a real RabbitMQ broker to the integration test environment. REST endpoints and event
 * publication are exercised against the broker; no local RabbitMQ installation is required.
 */
public abstract class AbstractMessagingIntegrationTest extends AbstractPostgresIntegrationTest {

    protected static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:4-management");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("spring.rabbitmq.template.mandatory", () -> "true");
        registry.add("spring.rabbitmq.connection-timeout", () -> "10s");
    }
}
