-- Complete the academic structure and workflows described in the design document.
ALTER TABLE students
    MODIFY COLUMN section_id BIGINT NULL,
    ADD COLUMN roll_number VARCHAR(40) NULL,
    ADD CONSTRAINT uk_students_section_roll UNIQUE (section_id, roll_number);

-- A faculty assignment is scoped to the section in which the subject is taught.
CREATE TABLE section_subject_faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_section_subject_faculty_section FOREIGN KEY (section_id) REFERENCES sections(id),
    CONSTRAINT fk_section_subject_faculty_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_section_subject_faculty_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT uk_ssf_section_subject UNIQUE (section_id, subject_id)
);

INSERT INTO section_subject_faculty (section_id, subject_id, faculty_id)
SELECT DISTINCT ts.section_id, ts.subject_id, ts.faculty_id
FROM timetable_slots ts
ON DUPLICATE KEY UPDATE faculty_id = VALUES(faculty_id);

-- Store absences as explicit rows too; a session can now describe each student's result.
ALTER TABLE attendance_records
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PRESENT',
    ADD COLUMN marked_by_user_id BIGINT NULL,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN latitude DECIMAL(10, 7) NULL,
    ADD COLUMN longitude DECIMAL(10, 7) NULL,
    ADD CONSTRAINT fk_records_marked_by FOREIGN KEY (marked_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT ck_records_status CHECK (status IN ('PRESENT', 'ABSENT', 'EXCUSED')),
    ADD CONSTRAINT ck_records_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_records_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);

ALTER TABLE attendance_records
    DROP CHECK ck_records_method,
    ADD CONSTRAINT ck_records_method CHECK (method IN ('QR', 'FACE', 'FINGERPRINT', 'MANUAL'));

CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    faculty_reviewer_id BIGINT NULL,
    admin_reviewer_id BIGINT NULL,
    admin_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at TIMESTAMP(6) NULL,
    reviewer_note VARCHAR(1000) NULL,
    CONSTRAINT fk_leave_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_leave_faculty FOREIGN KEY (faculty_reviewer_id) REFERENCES faculty(id),
    CONSTRAINT fk_leave_admin FOREIGN KEY (admin_reviewer_id) REFERENCES users(id),
    CONSTRAINT ck_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_leave_dates CHECK (end_date >= start_date)
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NULL,
    audience_role VARCHAR(16) NULL,
    notification_type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT ck_notifications_audience CHECK (recipient_user_id IS NOT NULL OR audience_role IS NOT NULL),
    CONSTRAINT ck_notifications_role CHECK (audience_role IS NULL OR audience_role IN ('STUDENT', 'FACULTY', 'ADMIN'))
);

CREATE TABLE attendance_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(80) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    updated_by_user_id BIGINT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_attendance_settings_key UNIQUE (setting_key),
    CONSTRAINT fk_attendance_settings_user FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
);

INSERT INTO attendance_settings (setting_key, setting_value) VALUES
    ('required_percentage', '75'),
    ('warning_percentage', '74'),
    ('critical_percentage', '65'),
    ('campus_latitude', ''),
    ('campus_longitude', ''),
    ('campus_radius_meters', '250'),
    ('long_leave_admin_approval_days', '3');

CREATE INDEX ix_section_subject_faculty_faculty ON section_subject_faculty(faculty_id, active);
CREATE INDEX ix_leave_student_status ON leave_requests(student_id, status, start_date);
CREATE INDEX ix_leave_faculty_status ON leave_requests(faculty_reviewer_id, status);
CREATE INDEX ix_notifications_recipient_created ON notifications(recipient_user_id, created_at);
CREATE INDEX ix_notifications_audience_created ON notifications(audience_role, created_at);
