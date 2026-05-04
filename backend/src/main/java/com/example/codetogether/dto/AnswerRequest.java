package com.example.codetogether.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 8000) String codeSnippet
) {
}
