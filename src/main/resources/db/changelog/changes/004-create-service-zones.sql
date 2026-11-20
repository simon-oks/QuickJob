--liquibase formatted sql

--changeset quickjob:004-create-service-zones
CREATE TABLE service_zones (
    id        UUID PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    boundary  geography(Polygon, 4326) NOT NULL,
    active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_service_zones_boundary ON service_zones USING GIST (boundary);
--rollback DROP TABLE service_zones;

--changeset quickjob:004-seed-lome-zone context:dev,test
INSERT INTO service_zones (id, name, boundary) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Lomé',
    ST_GeogFromText('SRID=4326;POLYGON((1.10 6.05, 1.30 6.05, 1.30 6.25, 1.10 6.25, 1.10 6.05))')
);
--rollback DELETE FROM service_zones WHERE id = 'a0000000-0000-0000-0000-000000000001';
