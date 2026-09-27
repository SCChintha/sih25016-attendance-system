package com.smartattend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_records_client_uuid", columnNames = "client_uuid"),
    @UniqueConstraint(name = "uk_records_session_student", columnNames = {"session_id", "student_id"})
}, indexes = @Index(name = "ix_records_student_time", columnList = "student_id,recorded_at"))
public class AttendanceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id") private AttendanceSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AttendanceMethod method;
    @Column(precision = 5, scale = 4) private BigDecimal confidence;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt = Instant.now();
    @Enumerated(EnumType.STRING) @Column(name = "sync_status", nullable = false, length = 16) private SyncStatus syncStatus = SyncStatus.SYNCED;
    @Column(name = "client_uuid", nullable = false, unique = true, length = 36) private String clientUuid;
    @Column(name = "device_id", length = 128) private String deviceId;
    protected AttendanceRecord() { }
    public AttendanceRecord(AttendanceSession session, Student student, AttendanceMethod method, String clientUuid) { this.session = session; this.student = student; this.method = method; this.clientUuid = clientUuid; }
    public Long getId() { return id; }
    public AttendanceSession getSession() { return session; }
    public Student getStudent() { return student; }
    public AttendanceMethod getMethod() { return method; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getClientUuid() { return clientUuid; }
}
