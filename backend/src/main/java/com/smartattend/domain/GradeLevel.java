package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "grade_levels")
public class GradeLevel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String name;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    protected GradeLevel() { }
    public GradeLevel(String name, int displayOrder) { this.name = name; this.displayOrder = displayOrder; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
