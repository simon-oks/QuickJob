--liquibase formatted sql

--changeset quickjob:011-create-worker-categories
CREATE TABLE worker_categories (
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category  VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, category)
);
--rollback DROP TABLE worker_categories;
