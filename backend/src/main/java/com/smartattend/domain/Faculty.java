package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true) private AppUser user;
    @Column(name = "subjects_locked", nullable = false, columnDefinition = "boolean default false") private boolean subjectsLocked = false;
    protected Faculty() { }
    public Faculty(AppUser user) { this.user = user; }
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public boolean isSubjectsLocked() { return subjectsLocked; }
    public void setSubjectsLocked(boolean subjectsLocked) { this.subjectsLocked = subjectsLocked; }
}
