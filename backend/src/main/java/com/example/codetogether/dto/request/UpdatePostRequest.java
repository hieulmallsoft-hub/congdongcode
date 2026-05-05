package com.example.codetogether.dto.request;

import com.example.codetogether.entity.PostCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdatePostRequest {
    @NotBlank
    @Size(max = 255)
    private String title;
    @NotBlank
    @Size(max = 5000)
    private String content;
    @Size(max = 8000)
    private String codeSnippet;
    @Size(max = 255)
    private String ImageUrl;
    @NotBlank
    @Size(max = 255)
    private String language;
    @Size(max = 255)
    private String tags;
    @NotNull
    private PostCategory category;
}
