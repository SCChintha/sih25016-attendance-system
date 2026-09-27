package com.smartattend.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattend.domain.*;
import com.smartattend.repository.*;
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
}
