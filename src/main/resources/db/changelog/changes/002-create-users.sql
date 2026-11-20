--liquibase formatted sql

--changeset quickjob:002-create-users
CREATE TABLE users (
    id               UUID PRIMARY KEY,
    phone_number     VARCHAR(20) NOT NULL UNIQUE,
    display_name     VARCHAR(100) NOT NULL,
    roles            VARCHAR(50) NOT NULL DEFAULT 'CLIENT,WORKER',
    phone_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_phone_number ON users (phone_number);
--rollback DROP TABLE users;
