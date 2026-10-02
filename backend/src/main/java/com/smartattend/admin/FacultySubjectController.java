package com.smartattend.admin;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class FacultySubjectController {
    private final FacultyRepository faculty;
    private final FacultySubjectRepository assignments;
    private final SubjectRepository subjects;
    private final AppUserRepository users;
    private final AuditLogRepository auditLogs;

    public FacultySubjectController(FacultyRepository faculty, FacultySubjectRepository assignments,
                                    SubjectRepository subjects, AppUserRepository users, AuditLogRepository auditLogs) {
        this.faculty = faculty; this.assignments = assignments; this.subjects = subjects; this.users = users; this.auditLogs = auditLogs;
    }

    @GetMapping("/api/faculty/subjects")
    @PreAuthorize("hasRole('FACULTY')")
    @Transactional(readOnly = true)
    public FacultySubjectsResponse getMySubjects(Authentication auth) {
        AppUser user = currentUser(auth);
        Faculty record = faculty.findByUserId(user.getId()).orElseThrow(() -> notFound("Faculty account not found"));
        return response(record);
    }

    @PostMapping("/api/faculty/subjects/confirm")
    @PreAuthorize("hasRole('FACULTY')")
    @Transactional
    public FacultySubjectsResponse confirmMySubjects(Authentication auth, @Valid @RequestBody SubjectSelection request) {
        AppUser user = currentUser(auth);
        Faculty record = faculty.findByUserIdForUpdate(user.getId()).orElseThrow(() -> notFound("Faculty account not found"));
        if (record.isSubjectsLocked()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Selection is already locked. Contact an Administrator to make changes.");
        List<Subject> selected = validatedSubjects(request.subjectIds());
        assignments.deleteByFacultyId(record.getId());
        selected.forEach(subject -> assignments.save(new FacultySubject(record, subject)));
        record.setSubjectsLocked(true);
        faculty.saveAndFlush(record);
        auditLogs.save(new AuditLog(user, "FACULTY_SUBJECT_SELECTION_LOCKED", null, "facultyId=" + record.getId() + ";subjectCount=" + selected.size()));
        return response(record);
    }

    @GetMapping("/api/admin/faculty/{facultyId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public FacultySubjectsResponse getFacultySubjects(@PathVariable Long facultyId) {
        Faculty record = faculty.findById(facultyId).orElseThrow(() -> notFound("Faculty account not found"));
        return response(record);
    }

    @GetMapping("/api/admin/faculty")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FacultyOption> listFaculty() {
        return faculty.findAll().stream()
            .filter(record -> record.getUser().getRole() == Role.FACULTY && record.getUser().isActive() && !record.getUser().isDeleted())
            .map(record -> new FacultyOption(record.getId(), record.getUser().getId(), record.getUser().getName(), record.getUser().getEmail(), record.isSubjectsLocked()))
            .sorted(java.util.Comparator.comparing(FacultyOption::name))
            .toList();
    }

    @PutMapping("/api/admin/faculty/{facultyId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FacultySubjectsResponse adminSetFacultySubjects(@PathVariable Long facultyId, Authentication authentication, @Valid @RequestBody SubjectSelection request) {
        Faculty record = faculty.findByIdForUpdate(facultyId).orElseThrow(() -> notFound("Faculty account not found"));
        List<Subject> selected = validatedSubjects(request.subjectIds());
        assignments.deleteByFacultyId(record.getId());
        selected.forEach(subject -> assignments.save(new FacultySubject(record, subject)));
        record.setSubjectsLocked(true);
        faculty.saveAndFlush(record);
        audit(authentication, "ADMIN_FACULTY_SUBJECTS_UPDATED", "facultyId=" + facultyId + ";subjectCount=" + selected.size());
        return response(record);
    }

    @PostMapping("/api/admin/faculty/{facultyId}/subjects/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FacultySubjectsResponse unlockFacultySubjects(@PathVariable Long facultyId, Authentication authentication) {
        Faculty record = faculty.findByIdForUpdate(facultyId).orElseThrow(() -> notFound("Faculty account not found"));
        record.setSubjectsLocked(false);
        faculty.saveAndFlush(record);
        audit(authentication, "ADMIN_FACULTY_SUBJECTS_UNLOCKED", "facultyId=" + facultyId);
        return response(record);
    }

    private List<Subject> validatedSubjects(List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject IDs must be unique");
        List<Subject> selected = subjects.findAllForUpdateByIdIn(ids);
        if (selected.size() != ids.size() || selected.stream().anyMatch(subject -> !subject.isActive() || subject.getStream() == null || !subject.getStream().isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selection contains a missing or inactive catalog subject");
        }
        return selected;
    }

    private FacultySubjectsResponse response(Faculty record) {
        List<SubjectDto> all = subjects.findByActiveTrueAndStreamIsNotNullOrderByNameAsc().stream().map(this::subjectDto).toList();
        List<SubjectDto> selected = assignments.findByFacultyId(record.getId()).stream().map(FacultySubject::getSubject).map(this::subjectDto).toList();
        return new FacultySubjectsResponse(record.getId(), record.getUser().getName(), record.isSubjectsLocked(), all, selected);
    }

    private SubjectDto subjectDto(Subject subject) {
        AcademicStream stream = subject.getStream();
        return new SubjectDto(subject.getId(), subject.getCode(), subject.getName(),
            stream.getGradeLevel().getId(), stream.getGradeLevel().getName(), stream.getId(), stream.getName());
    }
    private AppUser currentUser(Authentication auth) {
        return users.findByEmail(auth.getName()).filter(account -> !account.isDeleted() && account.isActive())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active account required"));
    }
    private void audit(Authentication authentication, String action, String metadata) {
        AppUser actor = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("Administrator account not found"));
        auditLogs.save(new AuditLog(actor, action, null, metadata));
    }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record SubjectSelection(@NotEmpty @Size(max = 100) List<@NotNull Long> subjectIds) { }
    public record SubjectDto(Long id, String code, String name, Long gradeId, String gradeName, Long streamId, String streamName) { }
    public record FacultySubjectsResponse(Long facultyId, String facultyName, boolean locked, List<SubjectDto> availableSubjects,
                                          List<SubjectDto> selectedSubjects) { }
    public record FacultyOption(Long id, Long userId, String name, String email, boolean locked) { }
}
