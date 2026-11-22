package {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import {{ cookiecutter.package_name }}.messaging.rabbitmq.RabbitMQProperties;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.producer.RabbitMQEventPublisher;

/**
 * Publishes outbox rows with publisher confirms. Rows are claimed with
 * {@code SKIP LOCKED}, making the relay safe to run in multiple application instances.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxMessageRepository repository;
    private final OutboxService outbox;
    private final RabbitMQEventPublisher publisher;
    private final RabbitMQProperties properties;

    public OutboxRelay(
            OutboxMessageRepository repository,
            OutboxService outbox,
            RabbitMQEventPublisher publisher,
            RabbitMQProperties properties) {
        this.repository = repository;
        this.outbox = outbox;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.integrations.rabbitmq.outbox-poll-interval-ms:2000}")
    @Transactional
    public void relay() {
        for (OutboxMessage row : repository.claimBatch(properties.getOutboxBatchSize())) {
            try {
                publisher.publish(outbox.toIntegrationMessage(row), row.getCorrelationId());
                row.markPublished(Instant.now());
            } catch (Exception exception) {
                row.recordFailure(exception);
                log.warn("Outbox publication failed for {} ({}): {}",
                        row.getType(), row.getMessageId(), exception.getMessage());
            }
        }
    }
}
