package com.smartattend.repository;

import com.smartattend.domain.AppUser;
import com.smartattend.domain.Department;
import com.smartattend.domain.Role;
import com.smartattend.domain.AttendanceMethod;
import com.smartattend.domain.AttendanceRecord;
import com.smartattend.domain.AttendanceSession;
import com.smartattend.domain.Faculty;
import com.smartattend.domain.Section;
import com.smartattend.domain.Student;
import com.smartattend.domain.Subject;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryConstraintsTest {
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired private AttendanceRecordRepository attendanceRecordRepository;

    @Test
    void findsDepartmentsByCode() {
        departmentRepository.saveAndFlush(new Department("CSE", "Computer Science"));

        assertThat(departmentRepository.findByCode("CSE")).isPresent();
    }

    @Test
    void enforcesUniqueUserEmail() {
        appUserRepository.save(new AppUser("One", "same@college.edu", "hash-one", Role.STUDENT));

        assertThatThrownBy(() -> appUserRepository.saveAndFlush(new AppUser("Two", "same@college.edu", "hash-two", Role.FACULTY)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesOneAttendanceRecordPerStudentPerSession() {
        Department department = departmentRepository.saveAndFlush(new Department("ECE", "Electronics"));
        Section section = sectionRepository.saveAndFlush(new Section(department, "A", (short) 1));
        AppUser facultyUser = appUserRepository.saveAndFlush(new AppUser("Faculty", "faculty@college.edu", "hash", Role.FACULTY));
        Faculty faculty = facultyRepository.saveAndFlush(new Faculty(facultyUser));
        AppUser studentUser = appUserRepository.saveAndFlush(new AppUser("Student", "student@college.edu", "hash", Role.STUDENT));
        Student student = studentRepository.saveAndFlush(new Student(studentUser, section));
        Subject subject = subjectRepository.saveAndFlush(new Subject(department, "ECE101", "Circuits"));
        AttendanceSession session = attendanceSessionRepository.saveAndFlush(new AttendanceSession(subject, section, faculty, Instant.now(), "seed"));
        attendanceRecordRepository.saveAndFlush(new AttendanceRecord(session, student, AttendanceMethod.QR, "00000000-0000-0000-0000-000000000001"));

        assertThatThrownBy(() -> attendanceRecordRepository.saveAndFlush(
            new AttendanceRecord(session, student, AttendanceMethod.QR, "00000000-0000-0000-0000-000000000002")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
