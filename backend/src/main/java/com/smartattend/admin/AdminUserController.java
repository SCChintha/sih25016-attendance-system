package com.smartattend.admin;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import org.springframework.security.core.Authentication;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AppUserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final GradeLevelRepository grades;
    private final AcademicStreamRepository streams;
    private final AuditLogRepository auditLogs;

    public AdminUserController(AppUserRepository users, StudentRepository students, FacultyRepository faculty,
                               GradeLevelRepository grades, AcademicStreamRepository streams, AuditLogRepository auditLogs) {
        this.users = users; this.students = students; this.faculty = faculty; this.grades = grades; this.streams = streams; this.auditLogs = auditLogs;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<UserSummary> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) Role role,
                                  @RequestParam(defaultValue = "") String search,
                                  @RequestParam(defaultValue = "false") boolean includeDeleted) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        String query = search.trim().toLowerCase(Locale.ROOT);
        Specification<AppUser> filter = (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) predicates.add(builder.equal(root.get("role"), role));
            if (!includeDeleted) predicates.add(builder.isNull(root.get("deletedAt")));
            if (!query.isBlank()) {
                String like = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), like, '\\'),
                    builder.like(builder.lower(root.get("email")), like, '\\')));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AppUser> result = users.findAll(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(this::summary);
    }

    @GetMapping("/{userId}")
    @Transactional(readOnly = true)
    public UserSummary get(@PathVariable Long userId) { return summary(findUser(userId)); }

    @PatchMapping("/{userId}")
    @Transactional
    public UserSummary update(@PathVariable Long userId, Authentication authentication, @Valid @RequestBody UserUpdate request) {
        AppUser user = users.findByIdForUpdate(userId).orElseThrow(() -> notFound("User not found"));
        if (user.isDeleted()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot be edited");
        Role nextRole = request.role() == null ? user.getRole() : request.role();
        if (request.name() != null) {
            if (request.name().isBlank()) throw badRequest("Name cannot be blank");
            user.setName(request.name().trim());
        }
        if (request.email() != null) {
            if (request.email().isBlank()) throw badRequest("Email cannot be blank");
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(user.getEmail()) && users.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId())).isPresent()) throw conflict("Email is already registered");
            if (!email.equals(user.getEmail())) { user.setEmail(email); user.revokeTokens(); }
        }
        if (nextRole != user.getRole()) {
            ensureAdminRemains(user, nextRole, user.isActive());
            user.setRole(nextRole);
            user.revokeTokens();
        }

        if (nextRole == Role.STUDENT) updateStudentProfile(user, request.gradeLevelId(), request.streamId());
        else if (request.gradeLevelId() != null || request.streamId() != null) throw badRequest("Grade and stream fields only apply to students");
        if (nextRole == Role.FACULTY) faculty.findByUserId(user.getId()).orElseGet(() -> faculty.save(new Faculty(user)));

        if (request.active() != null && request.active() != user.isActive()) {
            ensureAdminRemains(user, nextRole, request.active());
            user.setActive(request.active());
            user.revokeTokens();
        }
        AppUser saved = users.saveAndFlush(user);
        audit(authentication, "ADMIN_USER_PROFILE_UPDATED", saved.getId());
        return summary(saved);
    }

    @PatchMapping("/{userId}/status")
    @Transactional
    public UserSummary setActive(@PathVariable Long userId, Authentication authentication, @Valid @RequestBody StatusUpdate request) {
        AppUser user = users.findByIdForUpdate(userId).orElseThrow(() -> notFound("User not found"));
        if (user.isDeleted()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot be reactivated");
        if (request.active() != user.isActive()) {
            ensureAdminRemains(user, user.getRole(), request.active());
            user.setActive(request.active());
            user.revokeTokens();
        }
        AppUser saved = users.saveAndFlush(user);
        audit(authentication, request.active() ? "ADMIN_USER_REACTIVATED" : "ADMIN_USER_SUSPENDED", saved.getId());
        return summary(saved);
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public UserSummary softDelete(@PathVariable Long userId, Authentication authentication) {
        AppUser user = users.findByIdForUpdate(userId).orElseThrow(() -> notFound("User not found"));
        if (!user.isDeleted()) {
            ensureAdminRemains(user, user.getRole(), false);
            user.softDelete();
        }
        AppUser saved = users.saveAndFlush(user);
        audit(authentication, "ADMIN_USER_SOFT_DELETED", saved.getId());
        return summary(saved);
    }

    private void updateStudentProfile(AppUser user, Long requestedGradeId, Long requestedStreamId) {
        Student student = students.findByUserId(user.getId()).orElseGet(() -> students.save(new Student(user)));
        Long gradeId = requestedGradeId != null ? requestedGradeId : student.getGradeLevel() == null ? null : student.getGradeLevel().getId();
        Long streamId = requestedStreamId != null ? requestedStreamId : student.getStream() == null ? null : student.getStream().getId();
        if (gradeId == null || streamId == null) throw badRequest("A grade and stream are required for student accounts");
        GradeLevel grade = grades.findById(gradeId).filter(GradeLevel::isActive).orElseThrow(() -> badRequest("Selected grade is unavailable"));
        AcademicStream stream = streams.findById(streamId).filter(AcademicStream::isActive)
            .filter(value -> value.getGradeLevel().getId().equals(gradeId))
            .orElseThrow(() -> badRequest("Selected stream is not available for this grade"));
        student.setGradeLevel(grade);
        student.setStream(stream);
        students.save(student);
    }

    private UserSummary summary(AppUser user) {
        Student student = students.findByUserId(user.getId()).orElse(null);
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isActive(),
            user.isDeleted(), user.getCreatedAt(), student == null ? null : student.getAcademicGrade(),
            student == null || student.getGradeLevel() == null ? null : student.getGradeLevel().getId(),
            student == null || student.getStream() == null ? null : student.getStream().getId(),
            student == null || student.getStream() == null ? null : student.getStream().getName());
    }

    private AppUser findUser(Long id) { return users.findById(id).orElseThrow(() -> notFound("User not found")); }
    private void audit(Authentication authentication, String action, Long targetId) {
        AppUser actor = users.findByEmail(authentication.getName()).orElseThrow(() -> notFound("Administrator account not found"));
        auditLogs.save(new AuditLog(actor, action, null, "targetUserId=" + targetId));
    }
    private void ensureAdminRemains(AppUser user, Role nextRole, boolean nextActive) {
        if (user.getRole() == Role.ADMIN && user.isActive() && !user.isDeleted()
            && (nextRole != Role.ADMIN || !nextActive)
            && users.countByRoleAndActiveTrueAndDeletedAtIsNull(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active administrator must remain");
        }
    }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record UserUpdate(@Size(max = 160) String name, @Email @Size(max = 254) String email, Role role,
                             Long gradeLevelId, Long streamId, Boolean active) { }
    public record StatusUpdate(@NotNull Boolean active) { }
    public record UserSummary(Long id, String name, String email, String role, boolean active, boolean deleted,
                              java.time.Instant createdAt, String grade, Long gradeLevelId, Long streamId, String stream) { }
}
