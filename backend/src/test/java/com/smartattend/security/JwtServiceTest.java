package com.smartattend.security;

import com.smartattend.domain.AppUser;
import com.smartattend.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void createsTokenThatCanBeValidated() {
        JwtService service = new JwtService("test-secret-that-is-at-least-32-bytes-long", java.time.Duration.ofHours(1));
        AppUser user = new AppUser("Test User", "test@college.edu", "hash", Role.STUDENT);

        String token = service.createToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("test@college.edu");
        assertThat(service.isValid(token, "test@college.edu")).isTrue();
    }
}