CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT uk_departments_name UNIQUE (name)
);

CREATE TABLE sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    semester SMALLINT NOT NULL,
    CONSTRAINT fk_sections_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT uk_sections_department_name_semester UNIQUE (department_id, name, semester),
    CONSTRAINT ck_sections_semester CHECK (semester BETWEEN 1 AND 20)
);

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    CONSTRAINT fk_subjects_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT uk_subjects_code UNIQUE (code)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN'))
);

CREATE TABLE faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_faculty_user UNIQUE (user_id)
);

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    face_embedding_ref VARCHAR(512),
    qr_token VARCHAR(255),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_students_section FOREIGN KEY (section_id) REFERENCES sections(id),
    CONSTRAINT uk_students_user UNIQUE (user_id),
    CONSTRAINT uk_students_qr_token UNIQUE (qr_token)
);

CREATE TABLE timetable_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    day_of_week TINYINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(80) NOT NULL,
    CONSTRAINT fk_timetable_section FOREIGN KEY (section_id) REFERENCES sections(id),
    CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_timetable_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT ck_timetable_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_timetable_time CHECK (end_time > start_time)
);

CREATE TABLE attendance_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    timetable_slot_id BIGINT,
    start_time TIMESTAMP(6) NOT NULL,
    end_time TIMESTAMP(6),
    qr_seed VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    CONSTRAINT fk_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_sessions_section FOREIGN KEY (section_id) REFERENCES sections(id),
    CONSTRAINT fk_sessions_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT fk_sessions_slot FOREIGN KEY (timetable_slot_id) REFERENCES timetable_slots(id),
    CONSTRAINT ck_sessions_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_sessions_end_after_start CHECK (end_time IS NULL OR end_time >= start_time)
);

CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    method VARCHAR(16) NOT NULL,
    confidence DECIMAL(5, 4),
    recorded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sync_status VARCHAR(16) NOT NULL DEFAULT 'SYNCED',
    client_uuid CHAR(36) NOT NULL,
    device_id VARCHAR(128),
    CONSTRAINT fk_records_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),
    CONSTRAINT fk_records_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT uk_records_client_uuid UNIQUE (client_uuid),
    CONSTRAINT uk_records_session_student UNIQUE (session_id, student_id),
    CONSTRAINT ck_records_method CHECK (method IN ('QR', 'FACE', 'FINGERPRINT')),
    CONSTRAINT ck_records_sync_status CHECK (sync_status IN ('PENDING', 'SYNCED', 'FAILED')),
    CONSTRAINT ck_records_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE TABLE absenteeism_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_flags_student FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id BIGINT,
    action VARCHAR(120) NOT NULL,
    device_id VARCHAR(128),
    recorded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    metadata TEXT,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_audit_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
);

CREATE INDEX ix_sections_department ON sections(department_id);
CREATE INDEX ix_subjects_department ON subjects(department_id);
CREATE INDEX ix_students_section ON students(section_id);
CREATE INDEX ix_timetable_faculty_day ON timetable_slots(faculty_id, day_of_week);
CREATE INDEX ix_sessions_section_start ON attendance_sessions(section_id, start_time);
CREATE INDEX ix_sessions_faculty_start ON attendance_sessions(faculty_id, start_time);
CREATE INDEX ix_records_student_time ON attendance_records(student_id, recorded_at);
CREATE INDEX ix_flags_student_resolved ON absenteeism_flags(student_id, resolved);
CREATE INDEX ix_audit_user_time ON audit_log(user_id, recorded_at);