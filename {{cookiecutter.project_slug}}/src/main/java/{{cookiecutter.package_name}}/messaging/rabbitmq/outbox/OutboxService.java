package {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import {{ cookiecutter.package_name }}.messaging.rabbitmq.producer.IntegrationMessage;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes integration messages to the transactional outbox.
 */
@Service
public class OutboxService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final OutboxMessageRepository repository;
    private final JsonMapper jsonMapper;

    public OutboxService(OutboxMessageRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void enqueue(String type, UUID aggregateId, Instant occurredAt, Object payload) {
        String dedupeKey = type + ":" + aggregateId + ":" + occurredAt;
        if (repository.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        repository.save(new OutboxMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                aggregateId,
                occurredAt,
                jsonMapper.writeValueAsString(payload),
                dedupeKey,
                correlationId,
                Instant.now()));
    }

    public IntegrationMessage toIntegrationMessage(OutboxMessage row) {
        Map<String, Object> payload = jsonMapper.readValue(row.getPayloadJson(), MAP_TYPE);
        return new IntegrationMessage(
                row.getMessageId(),
                row.getType(),
                row.getAggregateId(),
                row.getOccurredAt(),
                payload);
    }
}
