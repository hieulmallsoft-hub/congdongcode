package com.example.codetogether.service;

import com.example.codetogether.dto.request.ChangePasswordRequest;
import com.example.codetogether.dto.request.UpdateProfileRequest;
import com.example.codetogether.dto.response.UserResponse;
import com.example.codetogether.entity.User;
import com.example.codetogether.exception.ApiException;
import com.example.codetogether.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        return toResponse(findUserById(id));
    }

    public UserResponse getUserByEmail(String email) {
        return toResponse(findUserByEmail(email));
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);
        user.setFullName(request.fullName().trim());
        user.setAvatarUrl(request.avatarUrl() == null ? "" : request.avatarUrl().trim());
        return toResponse(userRepository.save(user));
    }

    public void assertOwnerOrAdmin(Long userId, String requesterEmail, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        User user = findUserById(userId);
        if (!user.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to access this user");
        }
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse changeAvatar(String email, String avatarUrl) {
        User user = findUserByEmail(email);

        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar URL is required");
        }

        String cleanUrl = avatarUrl.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar URL is invalid");
        }

        user.setAvatarUrl(cleanUrl);
        return toResponse(userRepository.save(user));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserByEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAvatarUrl(), user.getRole());
    }
}
