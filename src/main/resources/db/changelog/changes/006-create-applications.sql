--liquibase formatted sql

--changeset quickjob:006-create-applications
CREATE TABLE applications (
    id           UUID PRIMARY KEY,
    mission_id   UUID NOT NULL REFERENCES missions(id),
    worker_id    UUID NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL,
    message      VARCHAR(500),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (mission_id, worker_id)
);
CREATE INDEX idx_applications_mission_id ON applications (mission_id);
CREATE INDEX idx_applications_worker_id ON applications (worker_id);
--rollback DROP TABLE applications;
