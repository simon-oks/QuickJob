--liquibase formatted sql

--changeset quickjob:013-create-notifications
CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    type       VARCHAR(30) NOT NULL,
    payload    JSONB NOT NULL,
    read       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read, created_at DESC);
--rollback DROP TABLE notifications;
