package com.example.codetogether.controller;

import com.example.codetogether.dto.AnswerRequest;
import com.example.codetogether.dto.AnswerResponse;
import com.example.codetogether.dto.ChallengeResponse;
import com.example.codetogether.dto.CommunityPostRequest;
import com.example.codetogether.dto.CommunityPostResponse;
import com.example.codetogether.dto.SubmissionRequest;
import com.example.codetogether.dto.SubmissionResponse;
import com.example.codetogether.security.CustomUserDetails;
import com.example.codetogether.service.CommunityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/feed")
    public List<CommunityPostResponse> getFeed() {
        return communityService.getFeed();
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityPostResponse createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return communityService.createPost(userDetails.getId(), request);
    }

    @PostMapping("/posts/{postId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerResponse addAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody AnswerRequest request
    ) {
        return communityService.addAnswer(userDetails.getId(), postId, request);
    }

    @GetMapping("/challenges/daily")
    public ChallengeResponse getDailyChallenge() {
        return communityService.getDailyChallenge();
    }

    @GetMapping("/challenges")
    public List<ChallengeResponse> getChallenges() {
        return communityService.getChallenges();
    }

    @PostMapping("/challenges/{challengeId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submitChallenge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long challengeId,
            @Valid @RequestBody SubmissionRequest request
    ) {
        return communityService.submitChallenge(userDetails.getId(), challengeId, request);
    }
}
