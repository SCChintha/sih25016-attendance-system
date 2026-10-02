package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "token_version", nullable = false, columnDefinition = "integer default 0") private int tokenVersion = 0;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected AppUser() { }
    public AppUser(String name, String email, String passwordHash, Role role) { this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = role; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public int getTokenVersion() { return tokenVersion; }
    public Instant getDeletedAt() { return deletedAt; }
    public boolean isDeleted() { return deletedAt != null; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void revokeTokens() { this.tokenVersion++; }
    public void softDelete() { this.deletedAt = Instant.now(); this.active = false; revokeTokens(); }
}
