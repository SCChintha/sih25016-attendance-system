-- Step 1: Add profile fields to students table
ALTER TABLE students ADD COLUMN academic_grade VARCHAR(80);
ALTER TABLE students ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

-- Step 2: Faculty-Subject mapping table
CREATE TABLE faculty_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    CONSTRAINT fk_fac_subj_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT fk_fac_subj_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT uk_faculty_subject UNIQUE (faculty_id, subject_id)
);

-- Step 3: Student-Subject-Faculty mapping table (Relational mapping for student onboarding choices)
CREATE TABLE student_subject_faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ssf_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_ssf_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_ssf_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id),
    CONSTRAINT uk_ssf_student_subject UNIQUE (student_id, subject_id)
);

CREATE INDEX ix_ssf_student ON student_subject_faculty(student_id);
CREATE INDEX ix_ssf_subject_faculty ON student_subject_faculty(subject_id, faculty_id);
