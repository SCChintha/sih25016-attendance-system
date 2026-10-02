package com.smartattend;

import com.smartattend.domain.AppUser;
import com.smartattend.domain.Role;
import com.smartattend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Configuration
public class AdminBootstrapConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
    CommandLineRunner createInitialAdmin(AppUserRepository users, PasswordEncoder passwords,
            @Value("${app.bootstrap.admin.email:}") String email,
            @Value("${app.bootstrap.admin.name:}") String name,
            @Value("${app.bootstrap.admin.password:}") String password) {
        return args -> bootstrap(users, passwords, email, name, password);
    }

    @Transactional
    void bootstrap(AppUserRepository users, PasswordEncoder passwords, String email, String name, String password) {
        if (email.isBlank() || name.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Set SUPER_ADMIN_EMAIL, SUPER_ADMIN_NAME and SUPER_ADMIN_PASSWORD (at least 12 characters) when APP_BOOTSTRAP_ADMIN_ENABLED=true");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var existing = users.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN || existing.get().isDeleted()) {
                throw new IllegalStateException("The configured super-admin email already belongs to a non-admin or deleted account");
            }
            return;
        }
        users.saveAndFlush(new AppUser(name.trim(), normalizedEmail, passwords.encode(password), Role.ADMIN));
    }
}
