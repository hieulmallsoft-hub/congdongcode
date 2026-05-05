package com.example.codetogether.dto.response;

import com.example.codetogether.entity.User;

public record UserResponse(Long id, String fullName, String email, String avatarUrl, User.Role role) {
}
