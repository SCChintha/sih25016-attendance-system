package com.smartattend.attendance;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated Controller handling Intermediate College Onboarding & Academic Mappings:
 * - Student Profile Setup (Stream, Year, Section & Subject-Lecturer Mappings)
 * - Faculty Profile Setup (Teaching Subject Catalog)
 * - Administrative Control Panel & Structural Log Records
 */
@RestController
@RequestMapping("/api")
public class PortalOnboardingController {

    private final SectionRepository sections;
    private final SubjectRepository subjects;
    private final FacultyRepository facultyRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final StudentRepository studentRepository;
    private final StudentSubjectFacultyRepository studentSubjectFacultyRepository;
    private final AppUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final GradeLevelRepository gradeLevels;
    private final AcademicStreamRepository streams;

    public PortalOnboardingController(
            SectionRepository sections,
            SubjectRepository subjects,
            FacultyRepository facultyRepository,
            FacultySubjectRepository facultySubjectRepository,
            StudentRepository studentRepository,
            StudentSubjectFacultyRepository studentSubjectFacultyRepository,
            AppUserRepository userRepository,
            DepartmentRepository departmentRepository,
            GradeLevelRepository gradeLevels,
            AcademicStreamRepository streams) {
        this.sections = sections;
        this.subjects = subjects;
        this.facultyRepository = facultyRepository;
        this.facultySubjectRepository = facultySubjectRepository;
        this.studentRepository = studentRepository;
        this.studentSubjectFacultyRepository = studentSubjectFacultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.gradeLevels = gradeLevels;
        this.streams = streams;
    }

    // ==========================================
    // 1. INTERMEDIATE STUDENT PORTAL ONBOARDING
    // ==========================================

    /**
     * POST /api/students/profile-setup
     * Saves student year, stream, section, and mapped lecturers for their subjects.
     */
    @PostMapping("/students/profile-setup")
    @Transactional
    public StudentProfileSetupResponse setupStudentProfile(
            Authentication authentication,
            @Valid @RequestBody StudentProfileSetupRequest request) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can perform student profile setup");
        }

        GradeLevel grade = gradeLevels.findById(request.gradeLevelId()).filter(GradeLevel::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected grade does not exist or is inactive"));
        AcademicStream stream = streams.findById(request.streamId()).filter(AcademicStream::isActive)
            .filter(candidate -> candidate.getGradeLevel().getId().equals(grade.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected stream is not active for this grade"));

        Section section = sections.findById(request.sectionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected section does not exist"));

        Student student = studentRepository.findByUserId(user.getId())
            .orElseGet(() -> new Student(user));

        student.setSection(section);
        student.setGradeLevel(grade);
        student.setStream(stream);
        student.setOnboardingCompleted(true);
        student = studentRepository.save(student);

        // Remove old mappings and save updated Student-Subject-Faculty mappings
        studentSubjectFacultyRepository.deleteByStudentId(student.getId());

        List<SubjectLecturerMapping> requestedMappings = request.mappings() == null ? List.of() : request.mappings();
        Set<Long> mappedSubjectIds = new HashSet<>();
        List<StudentSubjectFaculty> savedMappings = new ArrayList<>();
        for (SubjectLecturerMapping mapReq : requestedMappings) {
            if (!mappedSubjectIds.add(mapReq.subjectId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subject can only be mapped once");
            }
            Subject subject = subjects.findById(mapReq.subjectId())
                .filter(Subject::isActive)
                .filter(candidate -> candidate.getStream() != null && candidate.getStream().getId().equals(stream.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is not active for the selected stream: " + mapReq.subjectId()));
            Faculty faculty = facultyRepository.findById(mapReq.facultyId())
                .filter(candidate -> candidate.getUser().getRole() == Role.FACULTY
                    && candidate.getUser().isActive() && !candidate.getUser().isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected faculty member is unavailable: " + mapReq.facultyId()));
            if (!facultySubjectRepository.existsByFacultyIdAndSubjectId(faculty.getId(), subject.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected faculty member is not assigned to subject " + subject.getCode());
            }

            savedMappings.add(studentSubjectFacultyRepository.save(
                new StudentSubjectFaculty(student, subject, faculty)
            ));
        }

        return new StudentProfileSetupResponse(
            true,
            "Student profile setup completed successfully!",
            student.getId(),
            student.getAcademicGrade(),
            section.getName(),
            savedMappings.size()
        );
    }

    // Alias for legacy / onboarding submit endpoint compatibility
    @PostMapping("/students/onboarding/submit")
    @Transactional
    public StudentProfileSetupResponse submitStudentOnboarding(
            Authentication authentication,
            @Valid @RequestBody StudentProfileSetupRequest request) {
        return setupStudentProfile(authentication, request);
    }

    /**
     * GET /api/subjects/by-grade
     * Fetch board-provided subjects by section or stream/grade.
     */
    @GetMapping("/subjects/by-grade")
    @Transactional(readOnly = true)
    public List<SubjectInfo> getSubjectsByGrade(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String grade) {

        List<Subject> list;
        if (sectionId != null) {
            Optional<Section> sectionOpt = sections.findById(sectionId);
            if (sectionOpt.isPresent()) {
                list = subjects.findByDepartmentId(sectionOpt.get().getDepartment().getId());
                if (list.isEmpty()) {
                    list = subjects.findAll();
                }
            } else {
                list = subjects.findAll();
            }
        } else {
            list = subjects.findAll();
        }

        return list.stream().filter(Subject::isActive)
            .map(s -> new SubjectInfo(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDepartment() != null ? s.getDepartment().getName() : "General"
            ))
            .sorted(Comparator.comparing(SubjectInfo::code))
            .toList();
    }

    /**
     * GET /api/faculty/by-subject
     * Fetch faculty members registered for a specific subject (or all faculty if none registered yet).
     */
    @GetMapping("/faculty/by-subject")
    @Transactional(readOnly = true)
    public List<FacultyOption> getFacultyBySubject(@RequestParam Long subjectId) {
        List<FacultySubject> mapped = facultySubjectRepository.findBySubjectId(subjectId);
        Set<Faculty> set = mapped.stream().map(FacultySubject::getFaculty).collect(Collectors.toSet());

        return set.stream()
            .filter(f -> f.getUser().getRole() == Role.FACULTY && f.getUser().isActive() && !f.getUser().isDeleted())
            .map(f -> new FacultyOption(
                f.getId(),
                f.getUser().getName(),
                f.getUser().getEmail()
            ))
            .sorted(Comparator.comparing(FacultyOption::name))
            .toList();
    }


    // ==========================================
    // 2. FACULTY / LECTURER PORTAL ONBOARDING
    // ==========================================

    /**
     * POST /api/faculty/profile-setup
     * Saves lecturer's assigned teaching subject catalog.
     */
    @PostMapping("/faculty/profile-setup")
    @Transactional
    public FacultyProfileSetupResponse setupFacultyProfile(
            Authentication authentication,
            @Valid @RequestBody FacultyProfileSetupRequest request) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user.getRole() != Role.FACULTY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only faculty members can configure faculty profile");
        }

        Faculty faculty = facultyRepository.findByUserId(user.getId())
            .orElseGet(() -> facultyRepository.save(new Faculty(user)));

        if (faculty.isSubjectsLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selection is locked. Contact an Administrator to make changes.");
        }

        List<Subject> selected = subjects.findAllById(request.subjectIds());
        if (selected.size() != new HashSet<>(request.subjectIds()).size()
                || selected.stream().anyMatch(subject -> !subject.isActive() || subject.getStream() == null || !subject.getStream().isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose only active subjects configured in the academic catalog");
        }

        // Remove existing faculty-subject associations and re-create
        facultySubjectRepository.deleteByFacultyId(faculty.getId());

        List<SubjectInfo> assignedSubjects = new ArrayList<>();
        for (Subject subject : selected) {
            facultySubjectRepository.save(new FacultySubject(faculty, subject));
            assignedSubjects.add(new SubjectInfo(subject.getId(), subject.getCode(), subject.getName(), subject.getDepartment().getName()));
        }
        faculty.setSubjectsLocked(true);

        return new FacultyProfileSetupResponse(
            true,
            "Faculty subject catalog configured successfully!",
            faculty.getId(),
            user.getName(),
            assignedSubjects,
            faculty.isSubjectsLocked()
        );
    }

    /**
     * GET /api/faculty/profile-setup
     * Gets current faculty member's configured teaching subjects.
     */
    @GetMapping("/faculty/profile-setup")
    @Transactional(readOnly = true)
    public FacultyProfileSetupResponse getFacultyProfile(Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        if (user.getRole() != Role.FACULTY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only faculty members can view faculty profile setup");
        }

        Faculty faculty = facultyRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty record not found"));

        List<FacultySubject> list = facultySubjectRepository.findByFacultyId(faculty.getId());
        List<SubjectInfo> assignedSubjects = list.stream()
            .map(fs -> new SubjectInfo(
                fs.getSubject().getId(),
                fs.getSubject().getCode(),
                fs.getSubject().getName(),
                fs.getSubject().getDepartment() != null ? fs.getSubject().getDepartment().getName() : "General"
            ))
            .toList();

        return new FacultyProfileSetupResponse(
            true,
            "Faculty details retrieved",
            faculty.getId(),
            user.getName(),
            assignedSubjects,
            faculty.isSubjectsLocked()
        );
    }

    // ==========================================
    // 3. ADMINISTRATIVE CONTROL PANEL & LOGS
    // ==========================================

    /**
     * GET /api/admin/records
     * Administrative endpoint to review global structural logs, total counts, and student-lecturer mapping definitions.
     */
    @GetMapping("/admin/records")
    @Transactional(readOnly = true)
    public AdminRecordsResponse getAdminRecords(Authentication authentication) {
        requireAdmin(authentication);

        List<AppUser> currentUsers = userRepository.findAll().stream().filter(user -> !user.isDeleted()).toList();
        long totalUsers = currentUsers.size();
        long totalStudents = currentUsers.stream().filter(user -> user.getRole() == Role.STUDENT).count();
        long totalFaculty = currentUsers.stream().filter(user -> user.getRole() == Role.FACULTY).count();
        long totalSections = sections.count();
        long totalSubjects = subjects.count();

        List<StudentSubjectFaculty> allMappings = studentSubjectFacultyRepository.findAll();
        List<MappingDetail> mappingDetails = allMappings.stream()
            .filter(mapping -> mapping.getStudent().getUser().getRole() == Role.STUDENT
                && !mapping.getStudent().getUser().isDeleted()
                && mapping.getFaculty().getUser().getRole() == Role.FACULTY
                && !mapping.getFaculty().getUser().isDeleted())
            .map(m -> new MappingDetail(
                m.getId(),
                m.getStudent().getId(),
                m.getStudent().getUser().getName(),
                m.getStudent().getUser().getEmail(),
                m.getStudent().getAcademicGrade(),
                m.getStudent().getSection() != null ? m.getStudent().getSection().getName() : "Unassigned",
                m.getSubject().getId(),
                m.getSubject().getCode(),
                m.getSubject().getName(),
                m.getFaculty().getId(),
                m.getFaculty().getUser().getName(),
                m.getFaculty().getUser().getEmail(),
                m.getCreatedAt().toString()
            ))
            .sorted(Comparator.comparing(MappingDetail::studentName))
            .toList();

        List<StructuralLogEntry> structuralLogs = List.of(
            new StructuralLogEntry("SYSTEM_INIT", "Database schema initialized with Intermediate Streams (MPC, BiPC, CEC, MEC, HEC)", Instant.now().minusSeconds(86400).toString()),
            new StructuralLogEntry("MAPPING_AUDIT", "Active Student-Subject-Faculty mappings count: " + mappingDetails.size(), Instant.now().toString()),
            new StructuralLogEntry("ACADEMIC_CATALOG", "Master sections count: " + totalSections + " | Subjects count: " + totalSubjects, Instant.now().toString())
        );

        return new AdminRecordsResponse(
            totalUsers,
            totalStudents,
            totalFaculty,
            totalSections,
            totalSubjects,
            mappingDetails.size(),
            mappingDetails,
            structuralLogs
        );
    }

    /**
     * PUT /api/admin/mappings/{id}
     * Admin endpoint to override an incorrect Student-Lecturer mapping definition.
     */
    @PutMapping("/admin/mappings/{id}")
    @Transactional
    public MappingDetail overrideMapping(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminMappingOverrideRequest request) {

        requireAdmin(authentication);

        StudentSubjectFaculty existing = studentSubjectFacultyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping record not found: " + id));

        Faculty newFaculty = facultyRepository.findById(request.facultyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty not found: " + request.facultyId()));

        Subject newSubject = request.subjectId() != null
            ? subjects.findById(request.subjectId()).orElse(existing.getSubject())
            : existing.getSubject();

        // Re-save mapping with updated faculty / subject
        studentSubjectFacultyRepository.deleteById(id);
        StudentSubjectFaculty updated = studentSubjectFacultyRepository.save(
            new StudentSubjectFaculty(existing.getStudent(), newSubject, newFaculty)
        );

        return new MappingDetail(
            updated.getId(),
            updated.getStudent().getId(),
            updated.getStudent().getUser().getName(),
            updated.getStudent().getUser().getEmail(),
            updated.getStudent().getAcademicGrade(),
            updated.getStudent().getSection() != null ? updated.getStudent().getSection().getName() : "Unassigned",
            updated.getSubject().getId(),
            updated.getSubject().getCode(),
            updated.getSubject().getName(),
            updated.getFaculty().getId(),
            updated.getFaculty().getUser().getName(),
            updated.getFaculty().getUser().getEmail(),
            updated.getCreatedAt().toString()
        );
    }

    /**
     * DELETE /api/admin/mappings/{id}
     * Admin endpoint to delete a mapping definition.
     */
    @DeleteMapping("/admin/mappings/{id}")
    @Transactional
    public Map<String, Object> deleteMapping(Authentication authentication, @PathVariable Long id) {
        requireAdmin(authentication);
        if (!studentSubjectFacultyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mapping record not found: " + id);
        }
        studentSubjectFacultyRepository.deleteById(id);
        return Map.of("success", true, "message", "Mapping record " + id + " deleted successfully.");
    }

    // ==========================================
    // HELPER UTILITY & DTO RECORDS
    // ==========================================

    private AppUser getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireAdmin(Authentication auth) {
        AppUser user = getAuthenticatedUser(auth);
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator privileges required");
        }
    }

    // DTO Records
    public record SubjectLecturerMapping(@NotNull Long subjectId, @NotNull Long facultyId) { }

    public record StudentProfileSetupRequest(
        @NotNull Long gradeLevelId,
        @NotNull Long streamId,
        @NotNull Long sectionId,
        @Valid List<@NotNull @Valid SubjectLecturerMapping> mappings
    ) {
        // Alias accessor for `selections` if passed by legacy payload
        public List<SubjectLecturerMapping> mappings() {
            return mappings;
        }
    }

    public record StudentProfileSetupResponse(
        boolean success,
        String message,
        Long studentId,
        String academicGrade,
        String sectionName,
        int mappedSubjectsCount
    ) { }

    public record FacultyProfileSetupRequest(
        @NotEmpty List<Long> subjectIds,
        String qualification,
        String phone
    ) { }

    public record SubjectInfo(Long id, String code, String name, String department) { }
    public record FacultyOption(Long id, String name, String email) { }

    public record FacultyProfileSetupResponse(

        boolean success,
        String message,
        Long facultyId,
        String facultyName,
        List<SubjectInfo> assignedSubjects,
        boolean locked
    ) { }

    public record MappingDetail(
        Long id,
        Long studentId,
        String studentName,
        String studentEmail,
        String academicGrade,
        String sectionName,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long facultyId,
        String facultyName,
        String facultyEmail,
        String createdAt
    ) { }

    public record StructuralLogEntry(String category, String message, String timestamp) { }

    public record AdminRecordsResponse(
        long totalUsers,
        long totalStudents,
        long totalFaculty,
        long totalSections,
        long totalSubjects,
        long totalMappings,
        List<MappingDetail> mappings,
        List<StructuralLogEntry> logs
    ) { }

    public record AdminMappingOverrideRequest(Long subjectId, @NotNull Long facultyId) { }
}
