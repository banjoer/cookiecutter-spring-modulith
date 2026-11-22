-- Module: identity. This module owns identity_user_accounts.
CREATE TABLE identity_user_accounts
(
    id            UUID PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_identity_user_accounts_username UNIQUE (username),
    CONSTRAINT uk_identity_user_accounts_email UNIQUE (email)
);

CREATE INDEX idx_identity_user_accounts_created_at ON identity_user_accounts (created_at);
