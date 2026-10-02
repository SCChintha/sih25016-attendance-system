-- Additive migration: preserve attendance and user history while adding admin controls.
ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP(6) NULL;

ALTER TABLE faculty
    ADD COLUMN subjects_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE grade_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_grade_levels_name UNIQUE (name)
);

CREATE TABLE academic_streams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade_level_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_stream_grade FOREIGN KEY (grade_level_id) REFERENCES grade_levels(id),
    CONSTRAINT fk_stream_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT uk_stream_grade_code UNIQUE (grade_level_id, code)
);

ALTER TABLE subjects
    ADD COLUMN stream_id BIGINT NULL,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT fk_subject_stream FOREIGN KEY (stream_id) REFERENCES academic_streams(id);

ALTER TABLE students
    ADD COLUMN grade_level_id BIGINT NULL,
    ADD COLUMN stream_id BIGINT NULL,
    ADD CONSTRAINT fk_student_grade FOREIGN KEY (grade_level_id) REFERENCES grade_levels(id),
    ADD CONSTRAINT fk_student_stream FOREIGN KEY (stream_id) REFERENCES academic_streams(id);

CREATE INDEX ix_users_role_active ON users(role, active, deleted_at);
CREATE INDEX ix_subjects_stream_active ON subjects(stream_id, is_active);
CREATE INDEX ix_students_grade_stream ON students(grade_level_id, stream_id);
