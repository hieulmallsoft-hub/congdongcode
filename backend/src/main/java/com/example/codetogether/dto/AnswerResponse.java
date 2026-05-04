package com.example.codetogether.dto;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        String content,
        String codeSnippet,
        String authorName,
        LocalDateTime createdAt
) {
}
