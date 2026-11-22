package {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Idempotent-consumer inbox. The inbound message id is the primary key, so a redelivered AMQP
 * message is processed at most once.
 */
@Entity
@Table(name = "messaging_inbox")
public class InboxMessage {

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(nullable = false, length = 160)
    private String type;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected InboxMessage() {
        // for JPA
    }

    public InboxMessage(
            UUID messageId,
            String type,
            UUID aggregateId,
            String payloadJson,
            String correlationId,
            Instant receivedAt) {
        this.messageId = Objects.requireNonNull(messageId, "messageId");
        this.type = Objects.requireNonNull(type, "type");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getType() {
        return type;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
