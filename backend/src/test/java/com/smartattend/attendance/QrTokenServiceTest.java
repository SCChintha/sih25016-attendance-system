package com.smartattend.attendance;

import com.smartattend.domain.AttendanceSession;
import com.smartattend.domain.Department;
import com.smartattend.domain.Faculty;
import com.smartattend.domain.Role;
import com.smartattend.domain.Section;
import com.smartattend.domain.Subject;
import com.smartattend.domain.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class QrTokenServiceTest {
    @Test
    void rejectsTokenSignedForAnotherSession() throws Exception {
        Department department = new Department("CSE", "Computer Science");
        Section section = new Section(department, "A", (short) 1);
        Subject subject = new Subject(department, "CS101", "Programming");
        Faculty faculty = new Faculty(new AppUser("Faculty", "faculty@test", "hash", Role.FACULTY));
        AttendanceSession first = new AttendanceSession(subject, section, faculty, Instant.now(), "seed-one");
        AttendanceSession second = new AttendanceSession(subject, section, faculty, Instant.now(), "seed-two");

        QrTokenService service = new QrTokenService();

        assertThat(service.isValid(service.create(first), second)).isFalse();
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        Department department = new Department("IT", "Information Technology");
        Section section = new Section(department, "A", (short) 1);
        Subject subject = new Subject(department, "IT101", "Foundations");
        Faculty faculty = new Faculty(new AppUser("Faculty", "expired@test", "hash", Role.FACULTY));
        AttendanceSession session = new AttendanceSession(subject, section, faculty, Instant.now(), "expiry-seed");
        Field id = AttendanceSession.class.getDeclaredField("id"); id.setAccessible(true); id.set(session, 42L);
        String body = "42:" + Instant.now().minusSeconds(1).getEpochSecond();
        Method sign = QrTokenService.class.getDeclaredMethod("sign", String.class, String.class); sign.setAccessible(true);
        String token = body + "." + sign.invoke(new QrTokenService(), body, session.getQrSeed());

        assertThat(new QrTokenService().isValid(token, session)).isFalse();
    }
}
