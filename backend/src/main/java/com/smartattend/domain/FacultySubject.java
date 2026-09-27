package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty_subjects", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"faculty_id", "subject_id"})
})
public class FacultySubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    protected FacultySubject() { }

    public FacultySubject(Faculty faculty, Subject subject) {
        this.faculty = faculty;
        this.subject = subject;
    }

    public Long getId() { return id; }
    public Faculty getFaculty() { return faculty; }
    public Subject getSubject() { return subject; }
}
