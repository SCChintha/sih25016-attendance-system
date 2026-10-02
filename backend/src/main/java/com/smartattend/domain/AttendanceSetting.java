package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attendance_settings")
public class AttendanceSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "setting_key", nullable = false, unique = true, length = 80) private String key;
    @Column(name = "setting_value", nullable = false, length = 255) private String value;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by_user_id") private AppUser updatedBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected AttendanceSetting() { }
    public AttendanceSetting(String key, String value) { this.key = key; this.value = value; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public void updateValue(String value, AppUser updatedBy) {
        this.value = value; this.updatedBy = updatedBy; this.updatedAt = Instant.now();
    }
}
