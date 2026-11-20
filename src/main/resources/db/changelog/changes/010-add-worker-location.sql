--liquibase formatted sql

--changeset quickjob:010-add-worker-location
ALTER TABLE users ADD COLUMN last_known_location geography(Point, 4326);
ALTER TABLE users ADD COLUMN last_location_at TIMESTAMPTZ;
CREATE INDEX idx_users_location ON users USING GIST (last_known_location);
--rollback DROP INDEX idx_users_location;
--rollback ALTER TABLE users DROP COLUMN last_location_at;
--rollback ALTER TABLE users DROP COLUMN last_known_location;
