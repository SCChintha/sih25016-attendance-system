package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id") private Department department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stream_id") private AcademicStream stream;
    @Column(nullable = false, unique = true, length = 32) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true") private boolean active = true;
    protected Subject() { }
    public Subject(Department department, String code, String name) { this.department = department; this.code = code; this.name = name; }
    public Subject(Department department, AcademicStream stream, String code, String name) { this.department = department; this.stream = stream; this.code = code; this.name = name; }
    public Long getId() { return id; }
    public Department getDepartment() { return department; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public AcademicStream getStream() { return stream; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
