-- Removes only the sample data inserted by LocalDataInitializer.
-- Run from the backend directory with the H2 RUNSCRIPT command.
-- The existing MPC department stays because the manually created Grade 11/12
-- streams reference it. General section and user-entered student/admin accounts stay.
-- The one timestamp-named example student is a local test account and is removed.
-- This transaction will fail safely if sample subjects/sections have been used
-- by attendance sessions or timetable rows; review those rows before cleanup.

SET AUTOCOMMIT FALSE;

DELETE FROM section_subject_faculty
WHERE section_id IN (
    SELECT id FROM sections WHERE name IN (
        'MPC 1st Year - Sec A', 'MPC 1st Year - Sec B',
        'MPC 2nd Year - Sec A', 'MPC 2nd Year - Sec B',
        'BiPC 1st Year - Sec A', 'BiPC 2nd Year - Sec A',
        'MEC 1st Year - Sec A', 'CEC 1st Year - Sec A'
    )
) OR subject_id IN (
    SELECT id FROM subjects WHERE code IN (
        'MATH1A', 'MATH1B', 'MATH2A', 'MATH2B', 'PHY1', 'PHY2',
        'CHE1', 'CHE2', 'ENG1', 'SAN1', 'BOT1', 'BOT2', 'ZOO1',
        'ZOO2', 'ECO1', 'COM1', 'CIV1'
    )
) OR faculty_id IN (
    SELECT f.id FROM faculty f JOIN users u ON u.id = f.user_id
    WHERE u.email IN (
        'vk.murthy@smartattend.edu', 's.ramanujan@smartattend.edu',
        'cv.raman@smartattend.edu', 'k.sarojini@smartattend.edu',
        'ms.swaminathan@smartattend.edu', 'h.khorana@smartattend.edu',
        'rk.narayan@smartattend.edu'
    )
);

DELETE FROM student_subject_faculty
WHERE subject_id IN (
    SELECT id FROM subjects WHERE code IN (
        'MATH1A', 'MATH1B', 'MATH2A', 'MATH2B', 'PHY1', 'PHY2',
        'CHE1', 'CHE2', 'ENG1', 'SAN1', 'BOT1', 'BOT2', 'ZOO1',
        'ZOO2', 'ECO1', 'COM1', 'CIV1'
    )
) OR faculty_id IN (
    SELECT f.id FROM faculty f JOIN users u ON u.id = f.user_id
    WHERE u.email IN (
        'vk.murthy@smartattend.edu', 's.ramanujan@smartattend.edu',
        'cv.raman@smartattend.edu', 'k.sarojini@smartattend.edu',
        'ms.swaminathan@smartattend.edu', 'h.khorana@smartattend.edu',
        'rk.narayan@smartattend.edu'
    )
);

DELETE FROM faculty_subjects
WHERE faculty_id IN (
    SELECT f.id FROM faculty f JOIN users u ON u.id = f.user_id
    WHERE u.email IN (
        'vk.murthy@smartattend.edu', 's.ramanujan@smartattend.edu',
        'cv.raman@smartattend.edu', 'k.sarojini@smartattend.edu',
        'ms.swaminathan@smartattend.edu', 'h.khorana@smartattend.edu',
        'rk.narayan@smartattend.edu'
    )
) OR subject_id IN (
    SELECT id FROM subjects WHERE code IN (
        'MATH1A', 'MATH1B', 'MATH2A', 'MATH2B', 'PHY1', 'PHY2',
        'CHE1', 'CHE2', 'ENG1', 'SAN1', 'BOT1', 'BOT2', 'ZOO1',
        'ZOO2', 'ECO1', 'COM1', 'CIV1'
    )
);

DELETE FROM faculty
WHERE user_id IN (
    SELECT id FROM users WHERE email IN (
        'vk.murthy@smartattend.edu', 's.ramanujan@smartattend.edu',
        'cv.raman@smartattend.edu', 'k.sarojini@smartattend.edu',
        'ms.swaminathan@smartattend.edu', 'h.khorana@smartattend.edu',
        'rk.narayan@smartattend.edu'
    )
);

DELETE FROM students
WHERE user_id IN (
    SELECT id FROM users WHERE email = 'student-1789277928@example.com'
);

DELETE FROM users
WHERE email IN (
    'vk.murthy@smartattend.edu', 's.ramanujan@smartattend.edu',
    'cv.raman@smartattend.edu', 'k.sarojini@smartattend.edu',
    'ms.swaminathan@smartattend.edu', 'h.khorana@smartattend.edu',
    'rk.narayan@smartattend.edu'
) OR email = 'student-1789277928@example.com';

DELETE FROM subjects
WHERE code IN (
    'MATH1A', 'MATH1B', 'MATH2A', 'MATH2B', 'PHY1', 'PHY2',
    'CHE1', 'CHE2', 'ENG1', 'SAN1', 'BOT1', 'BOT2', 'ZOO1',
    'ZOO2', 'ECO1', 'COM1', 'CIV1'
);

DELETE FROM sections
WHERE name IN (
    'MPC 1st Year - Sec A', 'MPC 1st Year - Sec B',
    'MPC 2nd Year - Sec A', 'MPC 2nd Year - Sec B',
    'BiPC 1st Year - Sec A', 'BiPC 2nd Year - Sec A',
    'MEC 1st Year - Sec A', 'CEC 1st Year - Sec A'
);

DELETE FROM departments WHERE code IN ('BIPC', 'MEC', 'CEC');

COMMIT;
SET AUTOCOMMIT TRUE;
