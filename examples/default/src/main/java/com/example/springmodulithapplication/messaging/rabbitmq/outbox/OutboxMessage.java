package com.example.springmodulithapplication.messaging.rabbitmq.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Transactional outbox row. It is written in the same transaction as the event listener that
 * observes a domain event and is published by {@link OutboxRelay}.
 */
@Entity
@Table(
        name = "messaging_outbox",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_messaging_outbox_dedupe",
                columnNames = "dedupe_key"))
public class OutboxMessage {

    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false, unique = true)
    private UUID messageId;

    @Column(nullable = false, length = 160)
    private String type;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "dedupe_key", nullable = false, length = 512)
    private String dedupeKey;

    @Column(name = "correlation_id", nullable = false, length = 128)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    protected OutboxMessage() {
        // for JPA
    }

    public OutboxMessage(
            UUID id,
            UUID messageId,
            String type,
            UUID aggregateId,
            Instant occurredAt,
            String payloadJson,
            String dedupeKey,
            String correlationId,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.messageId = Objects.requireNonNull(messageId, "messageId");
        this.type = Objects.requireNonNull(type, "type");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        this.dedupeKey = Objects.requireNonNull(dedupeKey, "dedupeKey");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
        this.lastError = null;
    }

    public void recordFailure(Exception exception) {
        this.attempts++;
        String message = exception.getMessage();
        this.lastError = message == null ? exception.getClass().getSimpleName() : truncate(message);
    }

    private static String truncate(String value) {
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    public UUID getId() {
        return id;
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
