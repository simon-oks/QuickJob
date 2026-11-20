--liquibase formatted sql

--changeset quickjob:012-add-mission-notification-radius
ALTER TABLE missions ADD COLUMN notification_radius_meters INTEGER;
--rollback ALTER TABLE missions DROP COLUMN notification_radius_meters;
