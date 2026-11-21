--liquibase formatted sql

--changeset quickjob:017-add-email-auth
ALTER TABLE users
    ALTER COLUMN phone_number DROP NOT NULL,
    ADD COLUMN email VARCHAR(255) UNIQUE,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE users DROP COLUMN email_verified, DROP COLUMN email, ALTER COLUMN phone_number SET NOT NULL;
