package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests", indexes = {
    @Index(name = "ix_leave_student_status", columnList = "student_id,status,start_date"),
    @Index(name = "ix_leave_faculty_status", columnList = "faculty_reviewer_id,status")
})
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private LeaveStatus status = LeaveStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "faculty_reviewer_id") private Faculty facultyReviewer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "admin_reviewer_id") private AppUser adminReviewer;
    @Column(name = "admin_approval_required", nullable = false) private boolean adminApprovalRequired;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt = Instant.now();
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "reviewer_note", length = 1000) private String reviewerNote;
    protected LeaveRequest() { }
    public LeaveRequest(Student student, String reason, LocalDate startDate, LocalDate endDate, boolean adminApprovalRequired) {
        this.student = student; this.reason = reason; this.startDate = startDate; this.endDate = endDate;
        this.adminApprovalRequired = adminApprovalRequired;
    }
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public LeaveStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
