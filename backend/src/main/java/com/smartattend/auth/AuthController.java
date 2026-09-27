package com.smartattend.auth;

import com.smartattend.domain.AppUser;
import com.smartattend.domain.Faculty;
import com.smartattend.domain.Role;
import com.smartattend.domain.Section;
import com.smartattend.domain.Student;
import com.smartattend.repository.AppUserRepository;
import com.smartattend.repository.FacultyRepository;
import com.smartattend.repository.SectionRepository;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final SectionRepository sections;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users, JwtService jwtService,
                          PasswordEncoder passwordEncoder, StudentRepository students, FacultyRepository faculty,
                          SectionRepository sections) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.students = students;
        this.faculty = faculty;
        this.sections = sections;
    }

    @PostMapping("/login")
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
        if (request.role() == Role.STUDENT && request.sectionId() == null) {
            throw new RegistrationFieldException("sectionId", "Section is required for student accounts");
        }
        if (request.role() != Role.STUDENT && request.sectionId() != null) {
            throw new RegistrationFieldException("sectionId", "Section is only used for student accounts");
        }
        AppUser user = users.save(new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        if (request.role() == Role.STUDENT) {
            Section section = sections.findById(request.sectionId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not found"));
            students.save(new Student(user, section));
        } else if (request.role() == Role.FACULTY) {
            faculty.save(new Faculty(user));
        }
        return response(user);
    }

    @GetMapping("/me")
    public UserResponse me(org.springframework.security.core.Authentication authentication) {
        return userResponse(users.findByEmail(authentication.getName()).orElseThrow());
    }

    private LoginResponse response(AppUser user) {
        return new LoginResponse(jwtService.createToken(user), userResponse(user));
    }

    private UserResponse userResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RegisterRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 128) String password, @NotNull Role role,
                                  Long sectionId) { }
    public record LoginResponse(String token, UserResponse user) { }
    public record UserResponse(Long id, String name, String email, String role) { }
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