package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log", indexes = @Index(name = "ix_audit_user_time", columnList = "user_id,recorded_at"))
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id") private AttendanceSession session;
    @Column(nullable = false, length = 120) private String action;
    @Column(name = "device_id", length = 128) private String deviceId;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt = Instant.now();
    @Column(columnDefinition = "TEXT") private String metadata;
    protected AuditLog() { }
    public AuditLog(AppUser user, String action, String deviceId, String metadata) { this.user = user; this.action = action; this.deviceId = deviceId; this.metadata = metadata; }
    public Long getId() { return id; }
}