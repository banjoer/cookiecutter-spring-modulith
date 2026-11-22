-- Module: audit. This module owns audit_entries.
CREATE TABLE audit_entries
(
    id          UUID PRIMARY KEY,
    event_type  VARCHAR(160)  NOT NULL,
    aggregate_id UUID         NOT NULL,
    actor       VARCHAR(64)   NOT NULL,
    summary     VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_entries_recorded_at ON audit_entries (recorded_at DESC);
CREATE INDEX idx_audit_entries_aggregate_id ON audit_entries (aggregate_id);
