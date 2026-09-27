package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attendance_sessions", indexes = {
    @Index(name = "ix_sessions_section_start", columnList = "section_id,start_time"),
    @Index(name = "ix_sessions_faculty_start", columnList = "faculty_id,start_time")
})
public class AttendanceSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id") private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") private Section section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "faculty_id") private Faculty faculty;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "timetable_slot_id") private TimetableSlot timetableSlot;
    @Column(name = "start_time", nullable = false) private Instant startTime;
    @Column(name = "end_time") private Instant endTime;
    @Column(name = "qr_seed", nullable = false, length = 255) private String qrSeed;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SessionStatus status;
    protected AttendanceSession() { }
    public AttendanceSession(Subject subject, Section section, Faculty faculty, Instant startTime, String qrSeed) { this.subject = subject; this.section = section; this.faculty = faculty; this.startTime = startTime; this.qrSeed = qrSeed; this.status = SessionStatus.OPEN; }
    public AttendanceSession(Subject subject, Section section, Faculty faculty, TimetableSlot timetableSlot, Instant startTime, String qrSeed) { this(subject, section, faculty, startTime, qrSeed); this.timetableSlot = timetableSlot; }
    public Long getId() { return id; }
    public Subject getSubject() { return subject; }
    public Section getSection() { return section; }
    public Faculty getFaculty() { return faculty; }
    public TimetableSlot getTimetableSlot() { return timetableSlot; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Long getSectionId() { return section.getId(); }
    public Long getFacultyId() { return faculty.getId(); }
    public String getQrSeed() { return qrSeed; }
    public boolean isOpen() { return status == SessionStatus.OPEN; }
    public SessionStatus getStatus() { return status; }
    public void close(Instant endedAt) { this.endTime = endedAt; this.status = SessionStatus.CLOSED; }
}
