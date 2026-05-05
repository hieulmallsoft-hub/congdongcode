package com.example.codetogether.service;

import com.example.codetogether.dto.request.LoginRequest;
import com.example.codetogether.dto.request.RegisterRequest;
import com.example.codetogether.dto.response.AuthResponse;
import com.example.codetogether.dto.response.UserResponse;
import com.example.codetogether.entity.User;
import com.example.codetogether.exception.ApiException;
import com.example.codetogether.repository.UserRepository;
import com.example.codetogether.security.CustomUserDetails;
import com.example.codetogether.security.JwtService;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final String adminEmail;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.admin.email:}") String adminEmail
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.adminEmail = adminEmail;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(roleFor(email));
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(new CustomUserDetails(saved));
        return new AuthResponse(token, toResponse(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (user.getRole() != User.Role.ADMIN && roleFor(user.getEmail()) == User.Role.ADMIN) {
            user.setRole(User.Role.ADMIN);
            user = userRepository.save(user);
        }
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, toResponse(user));
    }

    private User.Role roleFor(String email) {
        return adminEmail != null && !adminEmail.isBlank() && adminEmail.equalsIgnoreCase(email)
                ? User.Role.ADMIN
                : User.Role.USER;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAvatarUrl(), user.getRole());
    }
}
