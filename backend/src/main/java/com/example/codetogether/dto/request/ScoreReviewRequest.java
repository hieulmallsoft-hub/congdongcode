package com.example.codetogether.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ScoreReviewRequest(
        boolean correct,
        @Min(0) @Max(20) Integer qualityScore,
        @Size(max = 1000) String reviewNote
) {
}
