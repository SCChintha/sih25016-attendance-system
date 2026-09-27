package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "student_subject_faculty", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "subject_id"})
})
public class StudentSubjectFaculty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected StudentSubjectFaculty() { }

    public StudentSubjectFaculty(Student student, Subject subject, Faculty faculty) {
        this.student = student;
        this.subject = subject;
        this.faculty = faculty;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Subject getSubject() { return subject; }
    public Faculty getFaculty() { return faculty; }
    public Instant getCreatedAt() { return createdAt; }
}
