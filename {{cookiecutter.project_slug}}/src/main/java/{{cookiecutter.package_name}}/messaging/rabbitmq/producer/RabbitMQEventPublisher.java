package {{ cookiecutter.package_name }}.messaging.rabbitmq.producer;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import {{ cookiecutter.package_name }}.messaging.rabbitmq.RabbitMQProperties;

/**
 * Confirmed AMQP producer. The transactional outbox relay is the normal caller; the class is also
 * useful for explicitly publishing an integration message.
 */
@Component
public class RabbitMQEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * Publishes and waits for a publisher confirm. A nack or an unroutable mandatory message is a
     * failure, which causes the outbox row to remain unpublished and be retried.
     */
    public void publish(IntegrationMessage message, String correlationId) {
        CorrelationData correlation = new CorrelationData(message.messageId().toString());
        MessagePostProcessor headers = amqpMessage -> {
            var messageProperties = amqpMessage.getMessageProperties();
            messageProperties.setMessageId(message.messageId().toString());
            messageProperties.setCorrelationId(correlationId);
            messageProperties.setType(message.type());
            messageProperties.setTimestamp(Date.from(message.occurredAt()));
            messageProperties.setHeader("x-aggregate-id", message.aggregateId().toString());
            return amqpMessage;
        };

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKey(),
                message,
                headers,
                correlation);

        try {
            var confirm = correlation.getFuture().get(properties.getPublisherConfirmTimeoutSeconds(), TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new IllegalStateException("RabbitMQ nacked message " + message.messageId() + ": " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned unroutable message " + message.messageId() + ": "
                                + correlation.getReturned().getReplyText());
            }
            log.debug("Published {} ({}) to {}", message.type(), message.messageId(), properties.getExchange());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a RabbitMQ publisher confirm.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish message " + message.messageId() + ".", exception);
        }
    }

}
