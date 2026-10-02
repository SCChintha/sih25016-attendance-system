package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "section_subject_faculty", uniqueConstraints =
    @UniqueConstraint(name = "uk_ssf_section_subject", columnNames = {"section_id", "subject_id"}))
public class SectionSubjectFaculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") private Section section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id") private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "faculty_id") private Faculty faculty;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected SectionSubjectFaculty() { }
    public SectionSubjectFaculty(Section section, Subject subject, Faculty faculty) {
        this.section = section; this.subject = subject; this.faculty = faculty;
    }
    public Long getId() { return id; }
    public Section getSection() { return section; }
    public Subject getSubject() { return subject; }
    public Faculty getFaculty() { return faculty; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
