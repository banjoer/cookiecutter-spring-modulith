package com.example.springmodulithapplication.messaging.rabbitmq.consumer;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.springmodulithapplication.messaging.rabbitmq.outbox.InboxMessage;
import com.example.springmodulithapplication.messaging.rabbitmq.outbox.InboxMessageRepository;
import com.example.springmodulithapplication.messaging.rabbitmq.producer.IntegrationMessage;

import tools.jackson.databind.json.JsonMapper;

/**
 * Example AMQP consumer. It is idempotent: the inbox primary key makes redelivery harmless.
 * Listener retry and dead-letter behavior are configured through standard
 * {@code spring.rabbitmq.listener.simple.*} properties.
 */
@Component
public class RabbitMQEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventConsumer.class);

    private final InboxMessageRepository inbox;
    private final JsonMapper jsonMapper;

    public RabbitMQEventConsumer(InboxMessageRepository inbox, JsonMapper jsonMapper) {
        this.inbox = inbox;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "#{@applicationEventsQueue.name}")
    @Transactional
    public void onMessage(IntegrationMessage message, Message amqpMessage) {
        String correlationId = amqpMessage.getMessageProperties().getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "missing";
        }
        MDC.put("correlationId", correlationId);
        try {
            if (inbox.existsById(message.messageId())) {
                log.debug("Ignoring duplicate message {}", message.messageId());
                return;
            }
            inbox.save(new InboxMessage(
                    message.messageId(),
                    message.type(),
                    message.aggregateId(),
                    jsonMapper.writeValueAsString(message.payload()),
                    correlationId,
                    Instant.now()));
            log.info("Consumed {} ({})", message.type(), message.messageId());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
