-- Hibernate maps the client UUID as a VARCHAR(36). Keep the versioned MySQL
-- schema consistent with that mapping so validation succeeds on every startup.
ALTER TABLE attendance_records
    MODIFY COLUMN client_uuid VARCHAR(36) NOT NULL;
