package com.example.springmodulithapplication.messaging.rabbitmq.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port for the idempotent consumer inbox.
 */
public interface InboxMessageRepository extends JpaRepository<InboxMessage, UUID> {

    List<InboxMessage> findByType(String type);
}
