package com.example.codetogether.dto.response;

import java.time.LocalDateTime;

public record LeaderboardEntryResponse(
        Long userId,
        String fullName,
        Integer totalScore,
        Long solvedCount,
        Integer bestSpeedRank,
        Integer streakDays,
        LocalDateTime latestSubmissionAt
) {
}
