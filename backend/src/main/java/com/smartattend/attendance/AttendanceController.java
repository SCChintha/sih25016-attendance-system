package com.smartattend.attendance;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AttendanceController {
    private final AppUserRepository users;
    private final FacultyRepository faculty;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final SectionRepository sections;
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final TimetableSlotRepository timetableSlots;
    private final QrTokenService qrTokens;
    private final QrCodeProvider qrProvider;

    public AttendanceController(AppUserRepository users, FacultyRepository faculty, StudentRepository students,
                                SubjectRepository subjects, SectionRepository sections,
                                AttendanceSessionRepository sessions, AttendanceRecordRepository records, TimetableSlotRepository timetableSlots,
                                QrTokenService qrTokens, QrCodeProvider qrProvider) {
        this.users = users; this.faculty = faculty; this.students = students; this.subjects = subjects;
        this.sections = sections; this.sessions = sessions; this.records = records; this.timetableSlots = timetableSlots; this.qrTokens = qrTokens; this.qrProvider = qrProvider;
    }

    @PostMapping("/sessions/start")
    @Transactional
    public SessionResponse start(@Valid @RequestBody StartSessionRequest request, Authentication authentication) {
        Faculty owner = currentFaculty(authentication);
        Subject subject = subjects.findById(request.subjectId()).orElseThrow(() -> notFound("Subject"));
        Section section = sections.findById(request.sectionId()).orElseThrow(() -> notFound("Section"));
        TimetableSlot slot = timetableSlots.findById(request.timetableSlotId()).orElseThrow(() -> notFound("Timetable slot"));
        if (!slot.getFaculty().getId().equals(owner.getId()) || !slot.getSubject().getId().equals(subject.getId()) || !slot.getSection().getId().equals(section.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session must use one of your assigned timetable slots");
        }
        boolean alreadyOpen = sessions.findByFaculty_IdAndStatus(owner.getId(), SessionStatus.OPEN).stream()
            .anyMatch(open -> open.getTimetableSlot() != null && open.getTimetableSlot().getId().equals(slot.getId()));
        if (alreadyOpen) throw new ResponseStatusException(HttpStatus.CONFLICT, "An attendance session is already open for this class");
        AttendanceSession session = sessions.save(new AttendanceSession(subject, section, owner, slot, Instant.now(), UUID.randomUUID().toString()));
        return response(session);
    }

    @PostMapping("/sessions/{id}/stop")
    @Transactional
    public SessionResponse stop(@PathVariable Long id, Authentication authentication) {
        Faculty owner = currentFaculty(authentication);
        AttendanceSession session = sessions.findById(id).orElseThrow(() -> notFound("Session"));
        if (!session.getStatus().equals(SessionStatus.OPEN)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not open");
        if (!session.getFacultyId().equals(owner.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session belongs to another faculty member");
        session.close(Instant.now());
        return response(session);
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse get(@PathVariable Long id, Authentication authentication) {
        AttendanceSession session = sessions.findById(id).orElseThrow(() -> notFound("Session"));
        AppUser user = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("User"));
        if (user.getRole() == Role.FACULTY && !faculty.findByUserId(user.getId()).map(owner -> owner.getId().equals(session.getFacultyId())).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session belongs to another faculty member");
        }
        if (user.getRole() == Role.STUDENT && !students.findByUserId(user.getId()).map(student -> student.belongsToSection(session.getSectionId())).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not enrolled in this section");
        }
        return response(session);
    }

    @PostMapping("/attendance/mark")
    @Transactional
    public AttendanceResponse mark(@Valid @RequestBody MarkAttendanceRequest request, Authentication authentication) {
        AppUser user = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("User"));
        Student student = students.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile required"));
        AttendanceSession session = sessions.findById(request.sessionId()).orElseThrow(() -> notFound("Session"));
        if (!session.isOpen() || !qrTokens.isValid(request.qrToken(), session)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired attendance QR");
        if (!student.belongsToSection(session.getSectionId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is not enrolled in this section");
        if (records.existsBySessionIdAndStudentId(session.getId(), student.getId()) || records.findByClientUuid(request.clientUuid()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded");
        }
        AttendanceResult result = qrProvider.capture(new AttendanceRequest(session.getId(), student.getId(), request.qrToken()));
        records.save(new AttendanceRecord(session, student, result.method(), request.clientUuid()));
        return new AttendanceResponse(session.getId(), student.getId(), result.method().name(), result.timestamp());
    }

    @GetMapping("/sessions/{id}/qr")
    public QrResponse qr(@PathVariable Long id, Authentication authentication) {
        AttendanceSession session = sessions.findById(id).orElseThrow(() -> notFound("Session"));
        Faculty owner = currentFaculty(authentication);
        if (!session.getFacultyId().equals(owner.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session belongs to another faculty member");
        if (!session.isOpen()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not open");
        return new QrResponse(session.getId(), qrTokens.create(session), Instant.now().plusSeconds(20));
    }

    private Faculty currentFaculty(Authentication authentication) {
        AppUser user = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("User"));
        return faculty.findByUserId(user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty profile required"));
    }
    private SessionResponse response(AttendanceSession session) { return new SessionResponse(session.getId(), session.getStatus().name()); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }

    public record StartSessionRequest(@NotNull Long subjectId, @NotNull Long sectionId, @NotNull Long timetableSlotId) { }
    public record MarkAttendanceRequest(@NotNull Long sessionId, @NotBlank String qrToken, @NotBlank String clientUuid) { }
    public record SessionResponse(Long id, String status) { }
    public record QrResponse(Long sessionId, String token, Instant expiresAt) { }
    public record AttendanceResponse(Long sessionId, Long studentId, String method, Instant timestamp) { }
}
