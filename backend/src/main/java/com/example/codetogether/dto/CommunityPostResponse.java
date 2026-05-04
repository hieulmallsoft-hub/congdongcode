package com.example.codetogether.dto;

import com.example.codetogether.entity.PostCategory;
import com.example.codetogether.entity.PostStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostResponse(
        Long id,
        String title,
        String body,
        String codeSnippet,
        String language,
        String tags,
        PostCategory category,
        PostStatus status,
        String authorName,
        LocalDateTime createdAt,
        long answerCount,
        List<AnswerResponse> answers
) {
}
