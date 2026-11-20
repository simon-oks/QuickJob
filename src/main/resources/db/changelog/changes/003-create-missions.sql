--liquibase formatted sql

--changeset quickjob:003-create-missions
CREATE TABLE missions (
    id               UUID PRIMARY KEY,
    client_id        UUID NOT NULL REFERENCES users(id),
    category         VARCHAR(30) NOT NULL,
    title            VARCHAR(100) NOT NULL,
    description      VARCHAR(1000),
    location         geography(Point, 4326) NOT NULL,
    address_label    VARCHAR(255) NOT NULL,
    start_at         TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL,
    amount_minor     BIGINT NOT NULL CHECK (amount_minor > 0),
    currency         CHAR(3) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_missions_location ON missions USING GIST (location);
CREATE INDEX idx_missions_status_start_at ON missions (status, start_at);
--rollback DROP TABLE missions;
