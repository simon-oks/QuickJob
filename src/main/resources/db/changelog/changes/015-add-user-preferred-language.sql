--liquibase formatted sql

--changeset quickjob:015-add-user-preferred-language
ALTER TABLE users ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'fr';
--rollback ALTER TABLE users DROP COLUMN preferred_language;
