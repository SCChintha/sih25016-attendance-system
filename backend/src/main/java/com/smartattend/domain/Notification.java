package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "ix_notifications_recipient_created", columnList = "recipient_user_id,created_at"),
    @Index(name = "ix_notifications_audience_created", columnList = "audience_role,created_at")
})
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_user_id") private AppUser recipient;
    @Enumerated(EnumType.STRING) @Column(name = "audience_role", length = 16) private Role audienceRole;
    @Column(name = "notification_type", nullable = false, length = 32) private String notificationType;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 2000) private String message;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id") private AppUser createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "read_at") private Instant readAt;
    protected Notification() { }
    public Notification(AppUser recipient, Role audienceRole, String type, String title, String message, AppUser createdBy) {
        this.recipient = recipient; this.audienceRole = audienceRole; this.notificationType = type;
        this.title = title; this.message = message; this.createdBy = createdBy;
    }
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { this.readAt = Instant.now(); }
}
