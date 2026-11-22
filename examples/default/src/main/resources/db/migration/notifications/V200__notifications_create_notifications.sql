-- Module: notifications. This module owns notifications.
CREATE TABLE notifications
(
    id                UUID PRIMARY KEY,
    recipient_user_id UUID         NOT NULL,
    recipient_username VARCHAR(64)  NOT NULL,
    subject           VARCHAR(160) NOT NULL,
    body              VARCHAR(2000) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_username, created_at DESC);
