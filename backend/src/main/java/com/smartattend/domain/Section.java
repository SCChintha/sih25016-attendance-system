package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sections", uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "name", "semester"}))
public class Section {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false) private short semester;
    protected Section() { }
    public Section(Department department, String name, short semester) { this.department = department; this.name = name; this.semester = semester; }
    public Long getId() { return id; }
    public Department getDepartment() { return department; }
    public String getName() { return name; }
    public short getSemester() { return semester; }
}