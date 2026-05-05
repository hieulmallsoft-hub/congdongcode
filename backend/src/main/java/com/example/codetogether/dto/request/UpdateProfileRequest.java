package com.example.codetogether.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 500) String avatarUrl
) {
}
