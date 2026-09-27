package com.smartattend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") private Section section;
    @Column(name = "face_embedding_ref", length = 512) private String faceEmbeddingRef;
    @Column(name = "qr_token", unique = true, length = 255) private String qrToken;
    protected Student() { }
    public Student(AppUser user, Section section) { this.user = user; this.section = section; }
    public Long getId() { return id; }
    public boolean belongsToSection(Long sectionId) { return section.getId().equals(sectionId); }
    public AppUser getUser() { return user; }
    public Section getSection() { return section; }
    public String getFaceEmbeddingRef() { return faceEmbeddingRef; }
    public String getQrToken() { return qrToken; }
}
