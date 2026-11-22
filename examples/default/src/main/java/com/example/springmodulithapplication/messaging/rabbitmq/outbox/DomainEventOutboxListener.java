package com.example.springmodulithapplication.messaging.rabbitmq.outbox;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.example.springmodulithapplication.identity.events.UserRegisteredEvent;
import com.example.springmodulithapplication.messaging.rabbitmq.RabbitMQProperties;
import com.example.springmodulithapplication.notifications.events.NotificationCreatedEvent;

/**
 * Externalizes selected public domain events through the transactional outbox.
 *
 * <p>This adapter depends only on named event interfaces. It never reaches into the identity or
 * notifications persistence models.</p>
 */
@Component
public class DomainEventOutboxListener {

    private final OutboxService outbox;
    private final RabbitMQProperties properties;

    public DomainEventOutboxListener(OutboxService outbox, RabbitMQProperties properties) {
        this.outbox = outbox;
        this.properties = properties;
    }

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        if (properties.isExternalizeDomainEvents()) {
            outbox.enqueue("identity.UserRegistered", event.userId(), event.registeredAt(), event);
        }
    }

    @ApplicationModuleListener
    void onNotificationCreated(NotificationCreatedEvent event) {
        if (properties.isExternalizeDomainEvents()) {
            outbox.enqueue("notifications.NotificationCreated", event.notificationId(), event.createdAt(), event);
        }
    }
}
