package com.example.springmodulithapplication.messaging.rabbitmq.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for the messaging outbox. The claim query uses PostgreSQL's
 * {@code FOR UPDATE SKIP LOCKED}, so multiple application instances can relay concurrently.
 */
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    Optional<OutboxMessage> findByDedupeKey(String dedupeKey);

    @Query(value = """
            select * from messaging_outbox
            where published_at is null
            order by created_at
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<OutboxMessage> claimBatch(@Param("batchSize") int batchSize);
}
