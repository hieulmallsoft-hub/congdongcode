package com.example.codetogether.dto;

import com.example.codetogether.entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String body,
        @Size(max = 8000) String codeSnippet,
        @NotBlank @Size(max = 40) String language,
        @Size(max = 160) String tags,
        @NotNull PostCategory category
) {
}
