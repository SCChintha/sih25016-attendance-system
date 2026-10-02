package com.smartattend.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattend.domain.*;
import com.smartattend.repository.*;
import com.smartattend.auth.AuthController;
import com.smartattend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JwtService jwtService;
    @Autowired private DepartmentRepository departments;
    @Autowired private SectionRepository sections;
    @Autowired private SubjectRepository subjects;
    @Autowired private AppUserRepository users;
    @Autowired private FacultyRepository faculty;
    @Autowired private StudentRepository students;
    @Autowired private AttendanceSessionRepository sessions;
    @Autowired private AttendanceRecordRepository attendanceRecords;
    @Autowired private QrTokenService qrTokens;

    private AppUser studentUser;
    private AttendanceSession session;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Department department = departments.save(new Department("T" + suffix, "Test Department " + suffix));
        Section section = sections.save(new Section(department, "A", (short) 1));
        AppUser facultyUser = users.save(new AppUser("Faculty", "faculty-" + suffix + "@test.edu", "hash", Role.FACULTY));
        Faculty facultyProfile = faculty.save(new Faculty(facultyUser));
        studentUser = users.save(new AppUser("Student", "student-" + suffix + "@test.edu", "hash", Role.STUDENT));
        Student studentProfile = students.save(new Student(studentUser, section));
        Subject subject = subjects.save(new Subject(department, "SUB" + suffix, "Test Subject"));
        session = sessions.save(new AttendanceSession(subject, section, facultyProfile, Instant.now(), "qr-seed-" + suffix));
    }

    @Test
    void recordsOnceAndRejectsDuplicateOrInvalidQr() throws Exception {
        String token = qrTokens.create(session);
        String authorization = "Bearer " + jwtService.createToken(studentUser);
        String first = json.writeValueAsString(new AttendanceController.MarkAttendanceRequest(session.getId(), token, UUID.randomUUID().toString()));
        String duplicate = json.writeValueAsString(new AttendanceController.MarkAttendanceRequest(session.getId(), token, UUID.randomUUID().toString()));
        String invalid = json.writeValueAsString(new AttendanceController.MarkAttendanceRequest(session.getId(), "invalid-token", UUID.randomUUID().toString()));

        mvc.perform(post("/api/attendance/mark").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(first)).andExpect(status().isOk());
        mvc.perform(post("/api/attendance/mark").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(duplicate)).andExpect(status().isConflict());
        mvc.perform(post("/api/attendance/mark").header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
    }

    @Test
    void adminDashboardIncludesStudentsBeforeOnboardingWithoutFailing() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser adminUser = users.save(new AppUser("Admin", "admin-dashboard-" + suffix + "@test.edu", "hash", Role.ADMIN));
        AppUser secondStudentUser = users.save(new AppUser("Second Student", "second-student-" + suffix + "@test.edu", "hash", Role.STUDENT));
        Student secondStudent = students.save(new Student(secondStudentUser, session.getSection()));
        AppUser unassignedUser = users.save(new AppUser("New Student", "new-student-" + suffix + "@test.edu", "hash", Role.STUDENT));
        students.save(new Student(unassignedUser));
        session.close(Instant.now());
        sessions.save(session);
        attendanceRecords.save(new AttendanceRecord(session, students.findByUserId(studentUser.getId()).orElseThrow(), AttendanceMethod.QR, UUID.randomUUID().toString()));
        AttendanceRecord absent = new AttendanceRecord(session, secondStudent, AttendanceMethod.MANUAL, UUID.randomUUID().toString());
        absent.setStatus(AttendanceStatus.ABSENT);
        attendanceRecords.save(absent);

        mvc.perform(get("/api/dashboard/admin")
                .header("Authorization", "Bearer " + jwtService.createToken(adminUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalStudents").value(3))
            .andExpect(jsonPath("$.overallAttendance").value(50.0))
            .andExpect(jsonPath("$.departments[0].students").value(2))
            .andExpect(jsonPath("$.departments[0].attendance").value(50.0))
            .andExpect(jsonPath("$.atRiskStudents.length()").value(1));
    }

    @Test
    void publicRegistrationCannotCreateAdministratorAccounts() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AuthController.RegisterRequest request = new AuthController.RegisterRequest(
            "Untrusted Admin", "untrusted-admin-" + suffix + "@test.edu", "valid-password", Role.ADMIN);

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
