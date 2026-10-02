package com.smartattend.admin;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
public class AcademicCatalogController {
    private final GradeLevelRepository grades;
    private final AcademicStreamRepository streams;
    private final SubjectRepository subjects;
    private final DepartmentRepository departments;
    private final AuditLogRepository auditLogs;
    private final AppUserRepository users;

    public AcademicCatalogController(GradeLevelRepository grades, AcademicStreamRepository streams,
                                     SubjectRepository subjects, DepartmentRepository departments, AuditLogRepository auditLogs,
                                     AppUserRepository users) {
        this.grades = grades; this.streams = streams; this.subjects = subjects; this.departments = departments;
        this.auditLogs = auditLogs; this.users = users;
    }

    @GetMapping("/api/public/academic/grades")
    @Transactional(readOnly = true)
    public List<GradeDto> publicGrades() {
        return grades.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(this::gradeDto).toList();
    }

    @GetMapping("/api/public/academic/grades/{gradeId}/streams")
    @Transactional(readOnly = true)
    public List<StreamDto> publicStreams(@PathVariable Long gradeId) {
        return streams.findByGradeLevelIdAndActiveTrueOrderByNameAsc(gradeId).stream().map(this::streamDto).toList();
    }

    @GetMapping("/api/public/academic/streams/{streamId}/subjects")
    @Transactional(readOnly = true)
    public List<SubjectDto> publicSubjects(@PathVariable Long streamId) {
        AcademicStream stream = streams.findById(streamId).filter(AcademicStream::isActive)
            .orElseThrow(() -> notFound("Stream not found or inactive"));
        return subjects.findByActiveTrueAndStream_IdOrderByNameAsc(stream.getId()).stream()
            .map(this::subjectDto).toList();
    }

    @GetMapping("/api/admin/academic/catalog")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public CatalogDto catalog() {
        List<GradeDto> gradeDtos = grades.findAll().stream().map(this::gradeDto).toList();
        List<StreamDto> streamDtos = streams.findAll().stream().map(this::streamDto).toList();
        List<SubjectDto> subjectDtos = subjects.findAll().stream().map(this::subjectDto).toList();
        return new CatalogDto(gradeDtos, streamDtos, subjectDtos);
    }

    @PostMapping("/api/admin/academic/grades")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public GradeDto createGrade(Authentication authentication, @Valid @RequestBody GradeRequest request) {
        String name = request.name().trim();
        if (grades.existsByNameIgnoreCase(name)) throw conflict("A grade with this name already exists");
        GradeLevel grade = grades.saveAndFlush(new GradeLevel(name, request.displayOrder() == null ? 0 : request.displayOrder()));
        audit(authentication, "ADMIN_GRADE_CREATED", "gradeId=" + grade.getId());
        return gradeDto(grade);
    }

    @PostMapping("/api/admin/academic/grades/{gradeId}/streams")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StreamDto createStream(@PathVariable Long gradeId, Authentication authentication, @Valid @RequestBody StreamRequest request) {
        GradeLevel grade = grades.findById(gradeId).orElseThrow(() -> notFound("Grade not found"));
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (streams.existsByGradeLevelIdAndCodeIgnoreCase(gradeId, code)) throw conflict("This stream code already exists for the selected grade");
        String departmentCode = code;
        Department department = departments.findByCode(departmentCode).orElseGet(() ->
            departments.saveAndFlush(new Department(departmentCode, request.name().trim())));
        AcademicStream stream = streams.saveAndFlush(new AcademicStream(grade, department, code, request.name().trim()));
        audit(authentication, "ADMIN_STREAM_CREATED", "streamId=" + stream.getId() + ";gradeId=" + gradeId);
        return streamDto(stream);
    }

    @PostMapping("/api/admin/academic/streams/{streamId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SubjectDto createSubject(@PathVariable Long streamId, Authentication authentication, @Valid @RequestBody SubjectRequest request) {
        AcademicStream stream = streams.findById(streamId).orElseThrow(() -> notFound("Stream not found"));
        if (!stream.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add a subject to an inactive stream");
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (subjects.existsByCodeIgnoreCase(code)) throw conflict("A subject with this code already exists");
        Subject subject = subjects.saveAndFlush(new Subject(stream.getDepartment(), stream, code, request.name().trim()));
        audit(authentication, "ADMIN_SUBJECT_CREATED", "subjectId=" + subject.getId() + ";streamId=" + streamId);
        return subjectDto(subject);
    }

    @PatchMapping("/api/admin/academic/subjects/{subjectId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SubjectDto setSubjectActive(@PathVariable Long subjectId, Authentication authentication, @Valid @RequestBody ActiveRequest request) {
        Subject subject = subjects.findById(subjectId).orElseThrow(() -> notFound("Subject not found"));
        subject.setActive(request.active());
        audit(authentication, request.active() ? "ADMIN_SUBJECT_ACTIVATED" : "ADMIN_SUBJECT_DEACTIVATED", "subjectId=" + subjectId);
        return subjectDto(subject);
    }

    private GradeDto gradeDto(GradeLevel grade) { return new GradeDto(grade.getId(), grade.getName(), grade.getDisplayOrder(), grade.isActive()); }
    private StreamDto streamDto(AcademicStream stream) {
        return new StreamDto(stream.getId(), stream.getGradeLevel().getId(), stream.getGradeLevel().getName(),
            stream.getCode(), stream.getName(), stream.isActive());
    }
    private SubjectDto subjectDto(Subject subject) {
        AcademicStream stream = subject.getStream();
        return new SubjectDto(subject.getId(), subject.getCode(), subject.getName(), subject.isActive(),
            subject.getDepartment() == null ? null : subject.getDepartment().getName(),
            stream == null ? null : stream.getId(), stream == null ? null : stream.getName(),
            stream == null ? null : stream.getGradeLevel().getName());
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void audit(Authentication authentication, String action, String metadata) {
        AppUser actor = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("Administrator account not found"));
        auditLogs.save(new AuditLog(actor, action, null, metadata));
    }

    public record GradeRequest(@NotBlank @Size(max = 80) String name, Integer displayOrder) { }
    public record StreamRequest(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 120) String name) { }
    public record SubjectRequest(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 160) String name) { }
    public record ActiveRequest(@NotNull Boolean active) { }
    public record GradeDto(Long id, String name, int displayOrder, boolean active) { }
    public record StreamDto(Long id, Long gradeId, String gradeName, String code, String name, boolean active) { }
    public record SubjectDto(Long id, String code, String name, boolean active, String department,
                             Long streamId, String streamName, String gradeName) { }
    public record CatalogDto(List<GradeDto> grades, List<StreamDto> streams, List<SubjectDto> subjects) { }
}
