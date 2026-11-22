package com.example.springmodulithapplication.audit.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springmodulithapplication.audit.AuditProperties;
import com.example.springmodulithapplication.audit.api.AuditApi;
import com.example.springmodulithapplication.audit.api.AuditEntryView;
import com.example.springmodulithapplication.audit.domain.AuditEntry;
import com.example.springmodulithapplication.audit.domain.AuditEntryRepository;
import com.example.springmodulithapplication.identity.events.UserRegisteredEvent;
import com.example.springmodulithapplication.notifications.events.NotificationCreatedEvent;

/**
 * Event-driven audit processor. It only uses public event contracts; it never references another
 * module's repositories or entities.
 */
@Service
public class AuditService implements AuditApi {

    private final AuditEntryRepository repository;
    private final AuditProperties properties;

    public AuditService(AuditEntryRepository repository, AuditProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        record(
                "identity.UserRegistered",
                event.userId(),
                event.username(),
                "User registered with username " + event.username(),
                event.registeredAt());
    }

    @ApplicationModuleListener
    void onNotificationCreated(NotificationCreatedEvent event) {
        record(
                "notifications.NotificationCreated",
                event.notificationId(),
                event.username(),
                "Notification created: " + event.subject(),
                event.createdAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryView> recent(int limit) {
        int capped = Math.max(1, Math.min(limit, properties.getMaxPageSize()));
        return repository.findAllByOrderByRecordedAtDesc(PageRequest.of(0, capped)).stream()
                .map(AuditService::toView)
                .toList();
    }

    void record(String eventType, UUID aggregateId, String actor, String summary, Instant occurredAt) {
        repository.save(new AuditEntry(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                actor,
                summary,
                occurredAt,
                Instant.now().truncatedTo(ChronoUnit.MICROS)));
    }

    private static AuditEntryView toView(AuditEntry entry) {
        return new AuditEntryView(
                entry.getId(),
                entry.getEventType(),
                entry.getAggregateId(),
                entry.getActor(),
                entry.getSummary(),
                entry.getOccurredAt(),
                entry.getRecordedAt());
    }
}
