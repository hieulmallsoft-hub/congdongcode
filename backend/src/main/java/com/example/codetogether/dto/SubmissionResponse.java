package com.example.codetogether.dto;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id,
        String answerCode,
        String notes,
        String authorName,
        LocalDateTime createdAt
) {
}
