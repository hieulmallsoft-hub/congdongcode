package com.example.codetogether.dto;

import java.time.LocalDate;
import java.util.List;

public record ChallengeResponse(
        Long id,
        String title,
        String prompt,
        String difficulty,
        String language,
        String starterCode,
        String expectedOutput,
        String solutionHint,
        LocalDate publishDate,
        long submissionCount,
        List<SubmissionResponse> recentSubmissions
) {
}
