package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, unique = true, length = 32) private String code;
    @Column(nullable = false, length = 160) private String name;
    protected Subject() { }
    public Subject(Department department, String code, String name) { this.department = department; this.code = code; this.name = name; }
    public Long getId() { return id; }
    public Department getDepartment() { return department; }
    public String getCode() { return code; }
    public String getName() { return name; }
}