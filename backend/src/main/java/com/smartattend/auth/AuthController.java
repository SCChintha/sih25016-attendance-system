package com.smartattend.auth;

import com.smartattend.domain.AppUser;
import com.smartattend.domain.Faculty;
import com.smartattend.domain.Role;
import com.smartattend.domain.Student;
import com.smartattend.repository.AppUserRepository;
import com.smartattend.repository.FacultyRepository;
import com.smartattend.repository.StudentRepository;
import com.smartattend.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
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
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return response(users.findByEmail(request.email()).orElseThrow());
    }

    @PostMapping("/register")
    @Transactional
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new RegistrationFieldException("email", "Email is already registered");
        }
        AppUser user = users.save(new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        if (request.role() == Role.STUDENT) {
            students.save(new Student(user));
        } else if (request.role() == Role.FACULTY) {
            faculty.save(new Faculty(user));
        }
        return response(user);
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserResponse me(org.springframework.security.core.Authentication authentication) {
        return userResponse(users.findByEmail(authentication.getName()).orElseThrow());
    }

    private LoginResponse response(AppUser user) {
        return new LoginResponse(jwtService.createToken(user), userResponse(user));
    }

    private UserResponse userResponse(AppUser user) {
        boolean onboardingCompleted = true;
        Long sectionId = null;
        String academicGrade = null;
        if (user.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = students.findByUserId(user.getId());
            if (studentOpt.isPresent()) {
                Student s = studentOpt.get();
                onboardingCompleted = s.isOnboardingCompleted();
                sectionId = s.getSection() != null ? s.getSection().getId() : null;
                academicGrade = s.getAcademicGrade();
            } else {
                onboardingCompleted = false;
            }
        }
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), onboardingCompleted, sectionId, academicGrade);
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RegisterRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 128) String password, @NotNull Role role) { }
    public record LoginResponse(String token, UserResponse user) { }
    public record UserResponse(Long id, String name, String email, String role, boolean onboardingCompleted, Long sectionId, String academicGrade) { }
    public record FieldException(String field, String message) { }

    public static class RegistrationFieldException extends RuntimeException {
        private final String field;
        public RegistrationFieldException(String field, String message) {
            super(message);
            this.field = field;
        }
        public String field() { return field; }
    }
}