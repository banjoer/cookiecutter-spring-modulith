-- Infrastructure adapter: messaging. These tables belong to the RabbitMQ adapter, not a business
-- module. The outbox is the reliable publication mechanism for critical integration events.
CREATE TABLE messaging_outbox
(
    id             UUID PRIMARY KEY,
    message_id     UUID         NOT NULL,
    type           VARCHAR(160) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json   TEXT         NOT NULL,
    dedupe_key     VARCHAR(512) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    attempts       INT          NOT NULL DEFAULT 0,
    last_error     VARCHAR(2000),
    CONSTRAINT uk_messaging_outbox_message_id UNIQUE (message_id),
    CONSTRAINT uk_messaging_outbox_dedupe UNIQUE (dedupe_key)
);

CREATE INDEX idx_messaging_outbox_pending ON messaging_outbox (published_at, created_at);

CREATE TABLE messaging_inbox
(
    message_id     UUID PRIMARY KEY,
    type           VARCHAR(160) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    payload_json   TEXT         NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    received_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
