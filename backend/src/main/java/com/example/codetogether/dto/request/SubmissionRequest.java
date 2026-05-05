package com.example.codetogether.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmissionRequest(
        @NotBlank @Size(max = 10000) String answerCode,
        @Size(max = 3000) String notes
) {
}
