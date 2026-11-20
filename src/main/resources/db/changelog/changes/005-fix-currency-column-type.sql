--liquibase formatted sql

--changeset quickjob:005-fix-currency-column-type
ALTER TABLE missions ALTER COLUMN currency TYPE VARCHAR(3);
--rollback ALTER TABLE missions ALTER COLUMN currency TYPE CHAR(3);
