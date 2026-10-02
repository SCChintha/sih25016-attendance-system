package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = true) @JoinColumn(name = "section_id", nullable = true) private Section section;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "grade_level_id") private GradeLevel gradeLevel;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stream_id") private AcademicStream stream;
    @Column(name = "academic_grade", length = 80) private String academicGrade;
    @Column(name = "onboarding_completed", nullable = false, columnDefinition = "boolean default false")
    private boolean onboardingCompleted = false;
    @Column(name = "face_embedding_ref", length = 512) private String faceEmbeddingRef;
    @Column(name = "qr_token", unique = true, length = 255) private String qrToken;

    protected Student() { }

    public Student(AppUser user) {
        this.user = user;
        this.section = null;
    }

    public Student(AppUser user, Section section) {
        this.user = user;
        this.section = section;
    }

    public Student(AppUser user, Section section, String academicGrade) {
        this.user = user;
        this.section = section;
        this.academicGrade = academicGrade;
    }

    public Student(AppUser user, GradeLevel gradeLevel, AcademicStream stream) {
        this.user = user;
        this.gradeLevel = gradeLevel;
        this.stream = stream;
        this.academicGrade = gradeLevel.getName();
        this.section = null;
    }

    public Long getId() { return id; }
    public boolean belongsToSection(Long sectionId) { return section != null && section.getId().equals(sectionId); }
    public AppUser getUser() { return user; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public String getAcademicGrade() { return academicGrade; }
    public void setAcademicGrade(String academicGrade) { this.academicGrade = academicGrade; }
    public GradeLevel getGradeLevel() { return gradeLevel; }
    public AcademicStream getStream() { return stream; }
    public void setGradeLevel(GradeLevel gradeLevel) { this.gradeLevel = gradeLevel; this.academicGrade = gradeLevel != null ? gradeLevel.getName() : null; }
    public void setStream(AcademicStream stream) { this.stream = stream; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public String getFaceEmbeddingRef() { return faceEmbeddingRef; }
    public String getQrToken() { return qrToken; }
}
