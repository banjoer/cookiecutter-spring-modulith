package com.example.springmodulithapplication.audit.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model for audit entries.
 */
public record AuditEntryView(
        UUID id,
        String eventType,
        UUID aggregateId,
        String actor,
        String summary,
        Instant occurredAt,
        Instant recordedAt) {
}
