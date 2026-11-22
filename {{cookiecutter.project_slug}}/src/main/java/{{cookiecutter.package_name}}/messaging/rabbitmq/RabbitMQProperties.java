package {{ cookiecutter.package_name }}.messaging.rabbitmq;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RabbitMQ AMQP integration configuration.
 *
 * <p>Connection details are intentionally not duplicated here. Use Spring Boot's standard
 * {@code spring.rabbitmq.*} properties for host, port, credentials, TLS, and listener retry
 * settings. This class owns only application-specific topology and outbox behavior.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.rabbitmq")
public class RabbitMQProperties {

    /**
     * Whether the RabbitMQ adapter, topology, producer, consumer, and outbox relay are active.
     */
    private boolean enabled;

    /**
     * Durable topic exchange used for application events.
     */
    @NotBlank
    private String exchange = "application.events";

    /**
     * Prefix for durable queue names.
     */
    @NotBlank
    private String queuePrefix = "application";

    /**
     * Routing key for domain events published to the exchange.
     */
    @NotBlank
    private String routingKey = "domain-events";

    /**
     * Whether public domain events are written to the transactional outbox and published to AMQP.
     */
    private boolean externalizeDomainEvents = true;

    /**
     * Delay between outbox relay runs.
     */
    @Min(100)
    private long outboxPollIntervalMs = 2_000;

    /**
     * Maximum number of outbox rows claimed per relay run.
     */
    @Min(1)
    private int outboxBatchSize = 50;

    /**
     * Maximum time to wait for a publisher confirm.
     */
    @Min(1)
    private int publisherConfirmTimeoutSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getQueuePrefix() {
        return queuePrefix;
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public boolean isExternalizeDomainEvents() {
        return externalizeDomainEvents;
    }

    public void setExternalizeDomainEvents(boolean externalizeDomainEvents) {
        this.externalizeDomainEvents = externalizeDomainEvents;
    }

    public long getOutboxPollIntervalMs() {
        return outboxPollIntervalMs;
    }

    public void setOutboxPollIntervalMs(long outboxPollIntervalMs) {
        this.outboxPollIntervalMs = outboxPollIntervalMs;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public int getPublisherConfirmTimeoutSeconds() {
        return publisherConfirmTimeoutSeconds;
    }

    public void setPublisherConfirmTimeoutSeconds(int publisherConfirmTimeoutSeconds) {
        this.publisherConfirmTimeoutSeconds = publisherConfirmTimeoutSeconds;
    }
}
