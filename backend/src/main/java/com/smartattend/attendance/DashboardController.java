package com.smartattend.attendance;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read models for the existing dashboards. Values are calculated from attendance tables. */
@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
public class DashboardController {
    private static final double AT_RISK_THRESHOLD = 75d;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a");
    private final AppUserRepository users;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final TimetableSlotRepository slots;
    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final StudentSubjectFacultyRepository studentSubjectFacultyRepository;

    public DashboardController(AppUserRepository users, StudentRepository students, FacultyRepository faculty,
                               TimetableSlotRepository slots, AttendanceSessionRepository sessions,
                               AttendanceRecordRepository records,
                               StudentSubjectFacultyRepository studentSubjectFacultyRepository) {
        this.users = users; this.students = students; this.faculty = faculty; this.slots = slots;
        this.sessions = sessions; this.records = records;
        this.studentSubjectFacultyRepository = studentSubjectFacultyRepository;
    }

    @GetMapping("/student")
    public StudentDashboardResponse student(Authentication authentication) {
        Student student = currentStudent(authentication);
        if (student.getSection() == null || !student.isOnboardingCompleted()) {
            return new StudentDashboardResponse(0.0, 0, 0, 0, List.of(), List.of());
        }
        List<AttendanceSession> conducted = conductedForSection(student.getSection().getId());
        List<AttendanceRecord> allRecords = records.findAll();
        long present = recordsForStudent(allRecords, student.getId(), conducted).size();
        List<TimetableSlot> sectionSlots = slots.findBySection_Id(student.getSection().getId());
        LocalDate today = LocalDate.now();
        List<ScheduleItem> schedule = sectionSlots.stream().filter(slot -> slot.getDayOfWeek() == today.getDayOfWeek().getValue())
            .sorted(Comparator.comparing(TimetableSlot::getStartTime)).map(slot -> {
                Optional<AttendanceSession> todaySession = sessions.findBySection_Id(student.getSection().getId()).stream()
                    .filter(session -> session.getTimetableSlot() != null && session.getTimetableSlot().getId().equals(slot.getId()))
                    .filter(session -> localDate(session.getStartTime()).equals(today)).findFirst();
                boolean marked = todaySession.isPresent() && allRecords.stream().anyMatch(record -> isPresent(record) && record.getStudent().getId().equals(student.getId()) && record.getSession().getId().equals(todaySession.get().getId()));
                return new ScheduleItem(slot.getId(), slot.getSubject().getName(), slot.getSubject().getCode(), CLOCK.format(slot.getStartTime()), slot.getRoom(), marked ? "present" : "upcoming");
            }).toList();

        Map<Long, List<AttendanceSession>> bySubject = conducted.stream().collect(Collectors.groupingBy(session -> session.getSubject().getId()));

        List<StudentSubjectFaculty> studentMappings = studentSubjectFacultyRepository.findByStudentId(student.getId());

        List<SubjectAttendance> subjectAttendance;
        if (!studentMappings.isEmpty()) {
            subjectAttendance = studentMappings.stream().map(ssf -> {
                Long subjectId = ssf.getSubject().getId();
                List<AttendanceSession> subjectSessions = bySubject.getOrDefault(subjectId, List.of());
                long subjectPresent = recordsForStudent(allRecords, student.getId(), subjectSessions).size();
                return new SubjectAttendance(
                    subjectId,
                    ssf.getSubject().getCode(),
                    ssf.getSubject().getName(),
                    ssf.getFaculty().getUser().getName(),
                    subjectPresent,
                    subjectSessions.size(),
                    percentage(subjectPresent, subjectSessions.size())
                );
            }).sorted(Comparator.comparing(SubjectAttendance::name)).toList();
        } else {
            Map<Long, TimetableSlot> subjectSlots = sectionSlots.stream().collect(Collectors.toMap(slot -> slot.getSubject().getId(), Function.identity(), (first, ignored) -> first));
            Set<Long> subjectIds = new LinkedHashSet<>(); subjectIds.addAll(subjectSlots.keySet()); subjectIds.addAll(bySubject.keySet());
            subjectAttendance = subjectIds.stream().map(subjectId -> {
                TimetableSlot slot = subjectSlots.get(subjectId);
                List<AttendanceSession> subjectSessions = bySubject.getOrDefault(subjectId, List.of());
                long subjectPresent = recordsForStudent(allRecords, student.getId(), subjectSessions).size();
                Subject subject = slot != null ? slot.getSubject() : subjectSessions.get(0).getSubject();
                String facultyName = slot != null ? slot.getFaculty().getUser().getName() : subjectSessions.get(0).getFaculty().getUser().getName();
                return new SubjectAttendance(subjectId, subject.getCode(), subject.getName(), facultyName, subjectPresent, subjectSessions.size(), percentage(subjectPresent, subjectSessions.size()));
            }).sorted(Comparator.comparing(SubjectAttendance::name)).toList();
        }

        long presentToday = schedule.stream().filter(item -> item.status().equals("present")).count();
        return new StudentDashboardResponse(percentage(present, conducted.size()), subjectAttendance.size(), schedule.size(), presentToday, schedule, subjectAttendance);
    }

    @GetMapping("/faculty")
    public FacultyDashboardResponse faculty(Authentication authentication) {
        Faculty owner = currentFaculty(authentication);
        List<TimetableSlot> ownSlots = slots.findByFaculty_Id(owner.getId());
        List<AttendanceSession> ownSessions = sessions.findByFaculty_Id(owner.getId());
        List<AttendanceSession> conducted = ownSessions.stream().filter(session -> session.getStatus() == SessionStatus.CLOSED).toList();
        List<AttendanceRecord> allRecords = records.findAll();
        long possible = conducted.stream().mapToLong(session -> students.countBySection_Id(session.getSection().getId())).sum();
        long present = allRecords.stream().filter(DashboardController::isPresent).filter(record -> conducted.stream().anyMatch(session -> session.getId().equals(record.getSession().getId()))).count();
        Map<Long, AttendanceSession> openBySlot = ownSessions.stream().filter(session -> session.getStatus() == SessionStatus.OPEN && session.getTimetableSlot() != null)
            .collect(Collectors.toMap(session -> session.getTimetableSlot().getId(), Function.identity(), (first, ignored) -> first));
        List<ClassSummary> classes = ownSlots.stream().map(slot -> {
            List<AttendanceSession> classSessions = conducted.stream().filter(session -> session.getTimetableSlot() != null && session.getTimetableSlot().getId().equals(slot.getId())).toList();
            long classPossible = classSessions.stream().mapToLong(session -> students.countBySection_Id(session.getSection().getId())).sum();
            long classPresent = allRecords.stream().filter(DashboardController::isPresent).filter(record -> classSessions.stream().anyMatch(session -> session.getId().equals(record.getSession().getId()))).count();
            return new ClassSummary(slot.getId(), slot.getSubject().getId(), slot.getSection().getId(), slot.getSubject().getCode(), slot.getSubject().getName(), slot.getSection().getName(),
                students.countBySection_Id(slot.getSection().getId()), dayLabel(slot.getDayOfWeek()) + " " + CLOCK.format(slot.getStartTime()) + "–" + CLOCK.format(slot.getEndTime()), slot.getRoom(), percentage(classPresent, classPossible), openBySlot.containsKey(slot.getId()) ? openBySlot.get(slot.getId()).getId() : null);
        }).toList();
        LocalDate today = LocalDate.now();
        List<FacultySession> todaySessions = ownSlots.stream().filter(slot -> slot.getDayOfWeek() == today.getDayOfWeek().getValue()).sorted(Comparator.comparing(TimetableSlot::getStartTime)).map(slot -> {
            AttendanceSession matched = ownSessions.stream().filter(session -> session.getTimetableSlot() != null && session.getTimetableSlot().getId().equals(slot.getId()))
                .filter(session -> localDate(session.getStartTime()).equals(today)).findFirst().orElse(null);
            long total = students.countBySection_Id(slot.getSection().getId());
            long sessionPresent = matched == null ? 0 : allRecords.stream().filter(DashboardController::isPresent).filter(record -> record.getSession().getId().equals(matched.getId())).count();
            String status = matched == null ? "upcoming" : matched.getStatus().name().toLowerCase();
            return new FacultySession(slot.getId(), matched == null ? null : matched.getId(), slot.getSubject().getName(), CLOCK.format(slot.getStartTime()), slot.getRoom(), status, sessionPresent, total, percentage(sessionPresent, total));
        }).toList();
        long totalStudents = ownSlots.stream().map(slot -> slot.getSection().getId()).distinct().mapToLong(students::countBySection_Id).sum();
        return new FacultyDashboardResponse(classes.size(), totalStudents, percentage(present, possible), todaySessions.size(), classes, todaySessions);
    }

    @GetMapping("/faculty/class")
    public ClassAttendanceResponse facultyClass(Long timetableSlotId, Authentication authentication) {
        Faculty owner = currentFaculty(authentication);
        TimetableSlot slot = slots.findById(timetableSlotId).orElseThrow(() -> notFound("Timetable slot"));
        if (!slot.getFaculty().getId().equals(owner.getId())) throw forbidden("Class belongs to another faculty member");
        List<AttendanceSession> classSessions = sessions.findByFaculty_Id(owner.getId()).stream().filter(session -> session.getTimetableSlot() != null && session.getTimetableSlot().getId().equals(slot.getId()))
            .filter(session -> session.getStatus() == SessionStatus.CLOSED).sorted(Comparator.comparing(AttendanceSession::getStartTime).reversed()).toList();
        List<AttendanceRecord> allRecords = records.findAll();
        long enrolled = students.countBySection_Id(slot.getSection().getId());
        List<SessionAttendance> sessionRows = classSessions.stream().map(session -> {
            long count = allRecords.stream().filter(DashboardController::isPresent).filter(record -> record.getSession().getId().equals(session.getId())).count();
            return new SessionAttendance(session.getId(), session.getStartTime(), count, enrolled, percentage(count, enrolled));
        }).toList();
        List<StudentAttendanceRow> studentRows = students.findBySection_Id(slot.getSection().getId()).stream().map(student -> {
            long count = recordsForStudent(allRecords, student.getId(), classSessions).size();
            return new StudentAttendanceRow(student.getId(), student.getUser().getName(), student.getUser().getEmail(), count, classSessions.size(), percentage(count, classSessions.size()));
        }).toList();
        long totalPresent = sessionRows.stream().mapToLong(SessionAttendance::presentStudents).sum();
        return new ClassAttendanceResponse(slot.getId(), slot.getSubject().getName(), slot.getSubject().getCode(), slot.getSection().getName(), classSessions.size(), enrolled, percentage(totalPresent, (long) enrolled * classSessions.size()), studentRows.stream().filter(row -> row.attendanceRate() < AT_RISK_THRESHOLD && row.totalClasses() > 0).count(), sessionRows, studentRows);
    }

    @GetMapping("/admin")
    public AdminDashboardResponse admin(Authentication authentication) {
        requireAdmin(authentication);
        List<Student> allStudents = students.findAll().stream()
            .filter(student -> student.getUser().getRole() == Role.STUDENT && !student.getUser().isDeleted())
            .toList();
        // Students can exist before completing onboarding, so their section is optional.
        // Keep them in the institution-wide student count, but only include assigned
        // students in section/department attendance analytics.
        List<Student> assignedStudents = allStudents.stream()
            .filter(student -> student.getSection() != null)
            .toList();
        List<AttendanceSession> allSessions = sessions.findAll();
        List<AttendanceSession> conducted = allSessions.stream().filter(session -> session.getStatus() == SessionStatus.CLOSED).toList();
        List<AttendanceRecord> allRecords = records.findAll();
        Set<Long> currentStudentIds = allStudents.stream().map(Student::getId).collect(Collectors.toSet());
        List<AttendanceRecord> currentStudentRecords = allRecords.stream()
            .filter(record -> currentStudentIds.contains(record.getStudent().getId()))
            .toList();
        long possible = conducted.stream().mapToLong(session -> activeStudentsInSection(allStudents, session.getSection().getId())).sum();
        List<DepartmentStats> departments = assignedStudents.stream().collect(Collectors.groupingBy(student -> student.getSection().getDepartment())).entrySet().stream().map(entry -> {
            Department department = entry.getKey();
            List<AttendanceSession> departmentSessions = conducted.stream().filter(session -> session.getSubject().getDepartment().getId().equals(department.getId())).toList();
            long departmentPossible = departmentSessions.stream().mapToLong(session -> activeStudentsInSection(allStudents, session.getSection().getId())).sum();
            long departmentPresent = currentStudentRecords.stream().filter(DashboardController::isPresent).filter(record -> departmentSessions.stream().anyMatch(session -> session.getId().equals(record.getSession().getId()))).count();
            long departmentFaculty = slots.findAll().stream().filter(slot -> slot.getSubject().getDepartment().getId().equals(department.getId()))
                .filter(slot -> slot.getFaculty().getUser().getRole() == Role.FACULTY && !slot.getFaculty().getUser().isDeleted())
                .map(slot -> slot.getFaculty().getId()).distinct().count();
            return new DepartmentStats(department.getName(), entry.getValue().size(), departmentFaculty, percentage(departmentPresent, departmentPossible));
        }).sorted(Comparator.comparing(DepartmentStats::name)).toList();
        List<TrendPoint> trend = monthlyTrend(conducted, currentStudentRecords, allStudents);
        List<DistributionPoint> distribution = distribution(assignedStudents, conducted, currentStudentRecords);
        List<AtRiskStudent> atRisk = assignedStudents.stream().map(student -> {
            List<AttendanceSession> studentSessions = conductedForSection(student.getSection().getId());
            long studentPresent = recordsForStudent(currentStudentRecords, student.getId(), studentSessions).size();
            return new AtRiskStudent(student.getId(), student.getUser().getName(), student.getSection().getDepartment().getName(), studentPresent, studentSessions.size(), percentage(studentPresent, studentSessions.size()));
        }).filter(student -> student.totalClasses() > 0 && student.attendance() < AT_RISK_THRESHOLD).sorted(Comparator.comparing(AtRiskStudent::attendance)).limit(20).toList();
        List<TopClass> topClasses = conducted.stream().collect(Collectors.groupingBy(session -> session.getSubject().getCode() + "|" + session.getSection().getName())).entrySet().stream().map(entry -> {
            List<AttendanceSession> group = entry.getValue(); AttendanceSession first = group.get(0);
            long groupPossible = group.stream().mapToLong(session -> activeStudentsInSection(allStudents, session.getSection().getId())).sum();
            long groupPresent = currentStudentRecords.stream().filter(DashboardController::isPresent).filter(record -> group.stream().anyMatch(session -> session.getId().equals(record.getSession().getId()))).count();
            return new TopClass(first.getSubject().getCode() + " - " + first.getSubject().getName(), first.getFaculty().getUser().getName(), percentage(groupPresent, groupPossible));
        }).sorted(Comparator.comparing(TopClass::attendance).reversed()).limit(10).toList();
        Set<Long> conductedIds = conducted.stream().map(AttendanceSession::getId).collect(Collectors.toSet());
        long conductedPresent = currentStudentRecords.stream().filter(DashboardController::isPresent).filter(record -> conductedIds.contains(record.getSession().getId())).count();
        long todayScans = currentStudentRecords.stream().filter(record -> record.getMethod() == AttendanceMethod.QR).filter(record -> localDate(record.getRecordedAt()).equals(LocalDate.now())).count();
        List<AppUser> currentUsers = users.findAll().stream().filter(user -> !user.isDeleted()).toList();
        long totalStudents = currentUsers.stream().filter(user -> user.getRole() == Role.STUDENT).count();
        long totalFaculty = currentUsers.stream().filter(user -> user.getRole() == Role.FACULTY).count();
        return new AdminDashboardResponse((int) totalStudents, (int) totalFaculty, allSessions.stream().filter(session -> session.getStatus() == SessionStatus.OPEN).count(), percentage(conductedPresent, possible), departments, trend, distribution, atRisk, topClasses, allSessions.stream().filter(session -> session.getStatus() == SessionStatus.OPEN).count(), todayScans);
    }

    private List<TrendPoint> monthlyTrend(List<AttendanceSession> conducted, List<AttendanceRecord> allRecords, List<Student> currentStudents) {
        YearMonth current = YearMonth.now(); List<TrendPoint> points = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) { YearMonth month = current.minusMonths(offset); List<AttendanceSession> monthSessions = conducted.stream().filter(session -> YearMonth.from(localDate(session.getStartTime())).equals(month)).toList(); long possible = monthSessions.stream().mapToLong(session -> activeStudentsInSection(currentStudents, session.getSection().getId())).sum(); long present = allRecords.stream().filter(DashboardController::isPresent).filter(record -> monthSessions.stream().anyMatch(session -> session.getId().equals(record.getSession().getId()))).count(); points.add(new TrendPoint(month.getMonth().toString().substring(0, 3), percentage(present, possible))); }
        return points;
    }
    private List<DistributionPoint> distribution(List<Student> allStudents, List<AttendanceSession> conducted, List<AttendanceRecord> allRecords) {
        long[] bins = new long[4]; long measured = 0;
        for (Student student : allStudents) { List<AttendanceSession> studentSessions = conductedForSection(student.getSection().getId()); if (studentSessions.isEmpty()) continue; measured++; double rate = percentage(recordsForStudent(allRecords, student.getId(), studentSessions).size(), studentSessions.size()); if (rate >= 90) bins[0]++; else if (rate >= 75) bins[1]++; else if (rate >= 60) bins[2]++; else bins[3]++; }
        return List.of(new DistributionPoint("Excellent (90-100%)", percentage(bins[0], measured)), new DistributionPoint("Good (75-89%)", percentage(bins[1], measured)), new DistributionPoint("Average (60-74%)", percentage(bins[2], measured)), new DistributionPoint("Poor (<60%)", percentage(bins[3], measured)));
    }
    private static long activeStudentsInSection(List<Student> currentStudents, Long sectionId) {
        return currentStudents.stream().filter(student -> student.getSection() != null && student.getSection().getId().equals(sectionId)).count();
    }
    private List<AttendanceSession> conductedForSection(Long sectionId) { return sessions.findBySection_Id(sectionId).stream().filter(session -> session.getStatus() == SessionStatus.CLOSED).toList(); }
    private List<AttendanceRecord> recordsForStudent(List<AttendanceRecord> source, Long studentId, List<AttendanceSession> candidates) { Set<Long> ids = candidates.stream().map(AttendanceSession::getId).collect(Collectors.toSet()); return source.stream().filter(DashboardController::isPresent).filter(record -> record.getStudent().getId().equals(studentId) && ids.contains(record.getSession().getId())).toList(); }
    private static boolean isPresent(AttendanceRecord record) { return record.getStatus() == AttendanceStatus.PRESENT; }
    private static double percentage(long numerator, long denominator) { return denominator == 0 ? 0d : Math.round((numerator * 10000d) / denominator) / 100d; }
    private static LocalDate localDate(Instant instant) { return instant.atZone(ZoneId.systemDefault()).toLocalDate(); }
    private static String dayLabel(byte day) { return DayOfWeek.of(day).toString().substring(0, 3); }
    private Student currentStudent(Authentication auth) { AppUser user = currentUser(auth); return students.findByUserId(user.getId()).orElseThrow(() -> forbidden("Student profile required")); }
    private Faculty currentFaculty(Authentication auth) { AppUser user = currentUser(auth); return faculty.findByUserId(user.getId()).orElseThrow(() -> forbidden("Faculty profile required")); }
    private void requireAdmin(Authentication auth) { if (currentUser(auth).getRole() != Role.ADMIN) throw forbidden("Administrator role required"); }
    private AppUser currentUser(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(() -> notFound("User")); }
    private ResponseStatusException notFound(String value) { return new ResponseStatusException(HttpStatus.NOT_FOUND, value + " not found"); }
    private ResponseStatusException forbidden(String value) { return new ResponseStatusException(HttpStatus.FORBIDDEN, value); }

    public record StudentDashboardResponse(double overallAttendance, int enrolledSubjects, int todayClasses, long presentToday, List<ScheduleItem> schedule, List<SubjectAttendance> subjects) { }
    public record ScheduleItem(Long timetableSlotId, String name, String code, String time, String room, String status) { }
    public record SubjectAttendance(Long subjectId, String code, String name, String faculty, long attendedClasses, long totalClasses, double attendance) { }
    public record FacultyDashboardResponse(int totalClasses, long totalStudents, double averageAttendance, int todaySessions, List<ClassSummary> classes, List<FacultySession> sessions) { }
    public record ClassSummary(Long timetableSlotId, Long subjectId, Long sectionId, String code, String name, String section, long enrolledStudents, String schedule, String room, double averageAttendance, Long activeSessionId) { }
    public record FacultySession(Long timetableSlotId, Long sessionId, String className, String time, String room, String status, long studentsPresent, long totalStudents, double attendanceRate) { }
    public record ClassAttendanceResponse(Long timetableSlotId, String name, String code, String section, int totalClasses, long enrolledStudents, double averageAttendance, long atRiskStudents, List<SessionAttendance> sessions, List<StudentAttendanceRow> students) { }
    public record SessionAttendance(Long sessionId, Instant startedAt, long presentStudents, long totalStudents, double attendanceRate) { }
    public record StudentAttendanceRow(Long id, String name, String email, long attendedClasses, long totalClasses, double attendanceRate) { }
    public record AdminDashboardResponse(int totalStudents, int totalFaculty, long activeClasses, double overallAttendance, List<DepartmentStats> departments, List<TrendPoint> monthlyAttendance, List<DistributionPoint> distribution, List<AtRiskStudent> atRiskStudents, List<TopClass> topClasses, long activeQrCodes, long dailyScans) { }
    public record DepartmentStats(String name, int students, long faculty, double attendance) { }
    public record TrendPoint(String month, double attendance) { }
    public record DistributionPoint(String name, double value) { }
    public record AtRiskStudent(Long id, String name, String department, long classesAttended, long totalClasses, double attendance) { }
    public record TopClass(String className, String faculty, double attendance) { }
}
