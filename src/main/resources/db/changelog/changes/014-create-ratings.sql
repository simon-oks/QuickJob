--liquibase formatted sql

--changeset quickjob:014-create-ratings
CREATE TABLE ratings (
    id          UUID PRIMARY KEY,
    mission_id  UUID NOT NULL REFERENCES missions(id),
    rater_id    UUID NOT NULL REFERENCES users(id),
    rated_id    UUID NOT NULL REFERENCES users(id),
    role_rated  VARCHAR(10) NOT NULL CHECK (role_rated IN ('CLIENT', 'WORKER')),
    score       INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment     VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ratings_unique_rater_mission UNIQUE (mission_id, rater_id),
    CONSTRAINT ratings_no_self CHECK (rater_id != rated_id)
);
CREATE INDEX idx_ratings_rated_role ON ratings (rated_id, role_rated);

ALTER TABLE missions ADD COLUMN completed_at TIMESTAMPTZ;
--rollback ALTER TABLE missions DROP COLUMN completed_at;
--rollback DROP TABLE ratings;
