package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "absenteeism_flags", indexes = @Index(name = "ix_flags_student_resolved", columnList = "student_id,resolved"))
public class AbsenteeismFlag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @Column(nullable = false, length = 255) private String reason;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private boolean resolved;
    protected AbsenteeismFlag() { }
    public AbsenteeismFlag(Student student, String reason) { this.student = student; this.reason = reason; }
    public Long getId() { return id; }
}