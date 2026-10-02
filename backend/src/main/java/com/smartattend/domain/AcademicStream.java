package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_streams", uniqueConstraints = @UniqueConstraint(columnNames = {"grade_level_id", "code"}))
public class AcademicStream {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "grade_level_id", nullable = false) private GradeLevel gradeLevel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(nullable = false, length = 32) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    protected AcademicStream() { }
    public AcademicStream(GradeLevel gradeLevel, Department department, String code, String name) {
        this.gradeLevel = gradeLevel; this.department = department; this.code = code; this.name = name;
    }
    public Long getId() { return id; }
    public GradeLevel getGradeLevel() { return gradeLevel; }
    public Department getDepartment() { return department; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
