--liquibase formatted sql

--changeset quickjob:001-enable-postgis
CREATE EXTENSION IF NOT EXISTS postgis;
--rollback DROP EXTENSION IF EXISTS postgis;
