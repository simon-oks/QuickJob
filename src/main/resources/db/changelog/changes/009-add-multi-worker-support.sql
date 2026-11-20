--liquibase formatted sql

--changeset quickjob:009-add-multi-worker-support
ALTER TABLE missions DROP COLUMN IF EXISTS worker_id;

ALTER TABLE missions ADD COLUMN required_workers INTEGER NOT NULL DEFAULT 1;
ALTER TABLE missions ADD CONSTRAINT chk_required_workers_positive CHECK (required_workers > 0);

CREATE TABLE mission_workers (
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    worker_id  UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (mission_id, worker_id)
);
CREATE INDEX idx_mission_workers_worker_id ON mission_workers (worker_id);
--rollback DROP TABLE mission_workers;
--rollback ALTER TABLE missions DROP CONSTRAINT chk_required_workers_positive;
--rollback ALTER TABLE missions DROP COLUMN required_workers;
--rollback ALTER TABLE missions ADD COLUMN worker_id UUID REFERENCES users(id);
