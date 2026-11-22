package com.example.springmodulithapplication.messaging.rabbitmq.producer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Stable wire format for messages crossing the AMQP boundary.
 */
public record IntegrationMessage(
        UUID messageId,
        String type,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> payload) {
}
