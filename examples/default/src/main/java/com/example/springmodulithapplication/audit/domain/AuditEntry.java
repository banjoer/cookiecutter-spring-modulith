package com.example.springmodulithapplication.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Immutable audit record. Audit data is owned only by the audit module.
 */
@Entity
@Table(name = "audit_entries")
public class AuditEntry {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 160)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected AuditEntry() {
        // for JPA
    }

    public AuditEntry(
            UUID id,
            String eventType,
            UUID aggregateId,
            String actor,
            String summary,
            Instant occurredAt,
            Instant recordedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
        this.actor = Objects.requireNonNull(actor, "actor");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getActor() {
        return actor;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
