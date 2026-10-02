package com.smartattend.auth;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import com.smartattend.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users, JwtService jwtService,
                          PasswordEncoder passwordEncoder, StudentRepository students, FacultyRepository faculty) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.students = students;
        this.faculty = faculty;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        AppUser user = users.findByEmail(email).filter(account -> !account.isDeleted())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return response(user);
    }

    @PostMapping("/register")
    @Transactional
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator accounts are created by the secure bootstrap process");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) throw new RegistrationFieldException("email", "Email is already registered");

        AppUser user;
        try {
            user = users.saveAndFlush(new AppUser(request.name().trim(), email,
                passwordEncoder.encode(request.password()), request.role()));
        } catch (DataIntegrityViolationException exception) {
            log.warn("Registration email uniqueness conflict role={} email={}", request.role(), email, exception);
            throw new RegistrationFieldException("email", "Email is already registered");
        }
        try {
            // A student profile is created during onboarding, when a section is selected.
            // This also keeps registration compatible with existing databases where section_id is NOT NULL.
            if (request.role() == Role.FACULTY) faculty.saveAndFlush(new Faculty(user));
        } catch (RuntimeException exception) {
            log.error("Registration persistence failed role={} email={}", request.role(), email, exception);
            throw exception;
        }
        return response(user);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserResponse me(org.springframework.security.core.Authentication authentication) {
        return users.findByEmail(authentication.getName()).filter(user -> !user.isDeleted())
            .map(this::userResponse).orElseThrow();
    }

    private LoginResponse response(AppUser user) {
        return new LoginResponse(jwtService.createToken(user), userResponse(user));
    }

    private UserResponse userResponse(AppUser user) {
        boolean onboardingCompleted = true;
        Long sectionId = null;
        String academicGrade = null;
        Long gradeLevelId = null;
        Long streamId = null;
        String streamName = null;
        if (user.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = students.findByUserId(user.getId());
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                onboardingCompleted = student.isOnboardingCompleted();
                sectionId = student.getSection() == null ? null : student.getSection().getId();
                academicGrade = student.getAcademicGrade();
                gradeLevelId = student.getGradeLevel() == null ? null : student.getGradeLevel().getId();
                streamId = student.getStream() == null ? null : student.getStream().getId();
                streamName = student.getStream() == null ? null : student.getStream().getName();
            } else onboardingCompleted = false;
        }
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
            onboardingCompleted, sectionId, academicGrade, gradeLevelId, streamId, streamName);
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank String password) { }
    public record RegisterRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 128) String password, @NotNull Role role) { }
    public record LoginResponse(String token, UserResponse user) { }
    public record UserResponse(Long id, String name, String email, String role, boolean onboardingCompleted,
                               Long sectionId, String academicGrade, Long gradeLevelId, Long streamId, String streamName) { }
    public record FieldException(String field, String message) { }

    public static class RegistrationFieldException extends RuntimeException {
        private final String field;
        public RegistrationFieldException(String field, String message) { super(message); this.field = field; }
        public String field() { return field; }
    }
}
