package com.example.springmodulithapplication.notifications.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springmodulithapplication.identity.events.UserRegisteredEvent;
import com.example.springmodulithapplication.notifications.api.NotificationApi;
import com.example.springmodulithapplication.notifications.api.NotificationView;
import com.example.springmodulithapplication.notifications.domain.Notification;
import com.example.springmodulithapplication.notifications.domain.NotificationRepository;
import com.example.springmodulithapplication.notifications.events.NotificationCreatedEvent;
import com.example.springmodulithapplication.shared.error.NotFoundException;

/**
 * Application service for notifications. It consumes identity events and publishes its own event
 * after persistence, without depending on any other module's internals.
 */
@Service
public class NotificationService implements NotificationApi {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    /**
     * Asynchronous, after-commit listener. Failures are tracked by Spring Modulith's event
     * publication registry instead of being silently swallowed.
     */
    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Notification notification = new Notification(
                UUID.randomUUID(),
                event.userId(),
                event.username(),
                "Welcome to the application",
                "Hello " + event.username() + ", your account is ready.",
                now);
        repository.save(notification);

        events.publishEvent(new NotificationCreatedEvent(
                notification.getId(),
                event.userId(),
                event.username(),
                notification.getSubject(),
                now));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationView> findByRecipient(String username) {
        return repository.findByRecipientUsernameOrderByCreatedAtDesc(username).stream()
                .map(NotificationService::toView)
                .toList();
    }

    @Override
    @Transactional
    public NotificationView markRead(UUID notificationId, String username) {
        Notification notification = repository.findByIdAndRecipientUsername(notificationId, username)
                .orElseThrow(() -> new NotFoundException("Notification was not found."));
        notification.markRead(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return toView(notification);
    }

    private static NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getSubject(),
                notification.getBody(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getReadAt() != null);
    }
}
