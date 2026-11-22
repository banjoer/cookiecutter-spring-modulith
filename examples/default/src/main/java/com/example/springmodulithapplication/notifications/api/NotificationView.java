package com.example.springmodulithapplication.notifications.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a notification.
 */
public record NotificationView(
        UUID id,
        String subject,
        String body,
        Instant createdAt,
        Instant readAt,
        boolean read) {
}
