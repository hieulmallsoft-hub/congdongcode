package com.example.codetogether.dto.response;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id,
        String answerCode,
        String notes,
        String authorName,
        Boolean correct,
        Integer qualityScore,
        Integer explanationScore,
        Integer speedRank,
        Integer speedBonus,
        Integer streakBonus,
        Integer finalScore,
        String reviewNote,
        LocalDateTime createdAt
) {
}
