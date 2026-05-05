package com.example.codetogether.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DailyChallengeRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String prompt,
        @NotBlank @Size(max = 32) String difficulty,
        @NotBlank @Size(max = 40) String language,
        @NotBlank @Size(max = 8000) String starterCode,
        @NotBlank @Size(max = 3000) String expectedOutput,
        @Size(max = 3000) String solutionHint,
        @NotNull LocalDate publishDate
) {
}
