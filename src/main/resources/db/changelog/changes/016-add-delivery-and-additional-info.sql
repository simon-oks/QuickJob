--liquibase formatted sql

--changeset quickjob:016-add-delivery-and-additional-info
ALTER TABLE missions
    ADD COLUMN delivery_location geography(Point, 4326),
    ADD COLUMN delivery_address_label VARCHAR(255),
    ADD COLUMN additional_info VARCHAR(2000);
CREATE INDEX idx_missions_delivery_location ON missions USING GIST (delivery_location);
--rollback DROP INDEX idx_missions_delivery_location; ALTER TABLE missions DROP COLUMN delivery_location, DROP COLUMN delivery_address_label, DROP COLUMN additional_info;
