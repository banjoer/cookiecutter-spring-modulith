package com.example.springmodulithapplication.notifications.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Notification aggregate. Notifications are owned exclusively by the notifications module.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "recipient_username", nullable = false, length = 64)
    private String recipientUsername;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
        // for JPA
    }

    public Notification(
            UUID id,
            UUID recipientUserId,
            String recipientUsername,
            String subject,
            String body,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.recipientUserId = Objects.requireNonNull(recipientUserId, "recipientUserId");
        this.recipientUsername = Objects.requireNonNull(recipientUsername, "recipientUsername");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.body = Objects.requireNonNull(body, "body");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = Objects.requireNonNull(readAt, "readAt");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
