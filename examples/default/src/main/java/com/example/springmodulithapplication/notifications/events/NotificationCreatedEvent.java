package com.example.springmodulithapplication.notifications.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a notification has been persisted. Audit and externalization adapters can
 * consume this event without accessing notification internals.
 */
public record NotificationCreatedEvent(
        UUID notificationId,
        UUID userId,
        String username,
        String subject,
        Instant createdAt) {
}
