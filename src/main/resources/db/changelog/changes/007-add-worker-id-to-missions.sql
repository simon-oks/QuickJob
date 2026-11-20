--liquibase formatted sql

--changeset quickjob:007-add-worker-id-to-missions
ALTER TABLE missions ADD COLUMN worker_id UUID REFERENCES users(id);
CREATE INDEX idx_missions_worker_id ON missions (worker_id);
--rollback ALTER TABLE missions DROP COLUMN worker_id;
