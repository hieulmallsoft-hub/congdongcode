package com.example.codetogether.controller;

import com.example.codetogether.dto.request.AnswerRequest;
import com.example.codetogether.dto.request.CommunityPostRequest;
import com.example.codetogether.dto.request.DailyChallengeRequest;
import com.example.codetogether.dto.request.ScoreReviewRequest;
import com.example.codetogether.dto.request.SubmissionRequest;
import com.example.codetogether.dto.response.AnswerResponse;
import com.example.codetogether.dto.response.ChallengeResponse;
import com.example.codetogether.dto.response.CommunityPostResponse;
import com.example.codetogether.dto.response.LeaderboardEntryResponse;
import com.example.codetogether.dto.response.SubmissionResponse;
import com.example.codetogether.helper.ApiResponse;
import com.example.codetogether.security.CustomUserDetails;
import com.example.codetogether.service.CommunityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<CommunityPostResponse>>> getFeed() {
        return ResponseEntity.ok(ApiResponse.success("Lay feed thanh cong", communityService.getFeed()));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityPostResponse>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Tao bai dang thanh cong", communityService.createPost(userDetails.getId(), request)));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostResponse>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        CommunityPostResponse response = communityService.updatePost(userDetails.getId(), isAdmin, postId, request);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat bai dang thanh cong", response));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId
    ) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        communityService.deletePost(userDetails.getId(), isAdmin, postId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Xoa bai dang thanh cong", null));
    }

    @PostMapping("/posts/{postId}/answers")
    public ResponseEntity<ApiResponse<AnswerResponse>> addAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody AnswerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Tao cau tra loi thanh cong", communityService.addAnswer(userDetails.getId(), postId, request)));
    }

    @GetMapping("/challenges/daily")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getDailyChallenge() {
        return ResponseEntity.ok(ApiResponse.success("Lay daily challenge thanh cong", communityService.getDailyChallenge()));
    }

    @GetMapping("/challenges")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getChallenges() {
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach challenge thanh cong", communityService.getChallenges()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long challengeId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Lay bang xep hang thanh cong", communityService.getLeaderboard(scope, challengeId)));
    }

    @PostMapping("/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChallengeResponse>> upsertDailyChallenge(
            @Valid @RequestBody DailyChallengeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Luu daily challenge thanh cong", communityService.upsertDailyChallenge(request)));
    }

    @PostMapping("/challenges/{challengeId}/submissions")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitChallenge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long challengeId,
            @Valid @RequestBody SubmissionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Nop bai thanh cong", communityService.submitChallenge(userDetails.getId(), challengeId, request)));
    }

    @PutMapping("/submissions/{submissionId}/score")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> reviewSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody ScoreReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat diem bai nop thanh cong", communityService.reviewSubmission(submissionId, request)));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityPostResponse>> getPostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success("Lay chi tiet bai dang thanh cong", communityService.getPostDetail(postId)));
    }
    
}
