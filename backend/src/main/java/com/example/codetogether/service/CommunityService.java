package com.example.codetogether.service;

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
import com.example.codetogether.entity.ChallengeSubmission;
import com.example.codetogether.entity.CommunityPost;
import com.example.codetogether.entity.DailyChallenge;
import com.example.codetogether.entity.PostAnswer;
import com.example.codetogether.entity.User;
import com.example.codetogether.exception.ApiException;
import com.example.codetogether.repository.ChallengeSubmissionRepository;
import com.example.codetogether.repository.CommunityPostRepository;
import com.example.codetogether.repository.DailyChallengeRepository;
import com.example.codetogether.repository.PostAnswerRepository;
import com.example.codetogether.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {
    private static final int BASE_CORRECT_SCORE = 50;
    private static final int EXPLANATION_BONUS = 10;
    private static final int DAILY_SUBMIT_BONUS = 5;
    private static final int STREAK_DAILY_BONUS = 5;

    private final CommunityPostRepository postRepository;
    private final PostAnswerRepository answerRepository;
    private final DailyChallengeRepository challengeRepository;
    private final ChallengeSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public CommunityService(
            CommunityPostRepository postRepository,
            PostAnswerRepository answerRepository,
            DailyChallengeRepository challengeRepository,
            ChallengeSubmissionRepository submissionRepository,
            UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.answerRepository = answerRepository;
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> getFeed() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(post -> toPostResponse(post, true))
                .toList();
    }

    @Transactional
    public CommunityPostResponse createPost(Long userId, CommunityPostRequest request) {
        User user = getUser(userId);
        CommunityPost post = new CommunityPost();
        applyPostRequest(post, request);
        post.setAuthor(user);
        return toPostResponse(postRepository.save(post), false);
    }

    @Transactional(readOnly = true)
    public CommunityPostResponse getPostDetail(Long postId) {
        return toPostResponse(getPost(postId), true);
    }

    @Transactional
    public CommunityPostResponse updatePost(Long userId, boolean isAdmin, Long postId, CommunityPostRequest request) {
        CommunityPost post = getPost(postId);
        assertPostOwnerOrAdmin(post, userId, isAdmin);
        applyPostRequest(post, request);
        return toPostResponse(postRepository.save(post), true);
    }

    @Transactional
    public void deletePost(Long userId, boolean isAdmin, Long postId) {
        CommunityPost post = getPost(postId);
        assertPostOwnerOrAdmin(post, userId, isAdmin);
        answerRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional
    public AnswerResponse addAnswer(Long userId, Long postId, AnswerRequest request) {
        User user = getUser(userId);
        CommunityPost post = getPost(postId);

        PostAnswer answer = new PostAnswer();
        answer.setPost(post);
        answer.setAuthor(user);
        answer.setContent(request.content());
        answer.setCodeSnippet(blankToNull(request.codeSnippet()));
        return toAnswerResponse(answerRepository.save(answer));
    }

    @Transactional(readOnly = true)
    public ChallengeResponse getDailyChallenge() {
        LocalDate today = LocalDate.now();
        DailyChallenge challenge = challengeRepository.findByPublishDate(today)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chua co daily challenge hom nay"));
        return toChallengeResponse(challenge);
    }

    @Transactional
    public ChallengeResponse upsertDailyChallenge(DailyChallengeRequest request) {
        DailyChallenge challenge = challengeRepository.findByPublishDate(request.publishDate())
                .orElseGet(DailyChallenge::new);
        applyChallengeRequest(challenge, request);
        return toChallengeResponse(challengeRepository.save(challenge));
    }

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getChallenges() {
        return challengeRepository.findAllByOrderByPublishDateDesc().stream()
                .map(this::toChallengeResponse)
                .toList();
    }

    @Transactional
    public SubmissionResponse submitChallenge(Long userId, Long challengeId, SubmissionRequest request) {
        User user = getUser(userId);
        DailyChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Challenge not found"));
        if (submissionRepository.existsByChallengeIdAndAuthorId(challengeId, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ban da nop bai cho challenge nay");
        }

        ChallengeSubmission submission = new ChallengeSubmission();
        submission.setChallenge(challenge);
        submission.setAuthor(user);
        submission.setAnswerCode(request.answerCode());
        submission.setNotes(blankToNull(request.notes()));
        applyAutomaticScore(submission);
        return toSubmissionResponse(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(String scope, Long challengeId) {
        List<ChallengeSubmission> submissions = "daily".equalsIgnoreCase(scope) && challengeId != null
                ? submissionRepository.findByChallengeIdOrderByFinalScoreDescCreatedAtAsc(challengeId).stream()
                        .filter(submission -> Boolean.TRUE.equals(submission.getCorrect()))
                        .toList()
                : submissionRepository.findByCorrectTrueOrderByFinalScoreDescCreatedAtAsc();
        return buildLeaderboard(submissions);
    }

    @Transactional
    public SubmissionResponse reviewSubmission(Long submissionId, ScoreReviewRequest request) {
        ChallengeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        submission.setCorrect(request.correct());
        submission.setQualityScore(request.qualityScore() == null ? 0 : request.qualityScore());
        submission.setReviewNote(blankToNull(request.reviewNote()));
        submission.setReviewedAt(LocalDateTime.now());
        applyFinalScore(submission);
        return toSubmissionResponse(submissionRepository.save(submission));
    }

    private CommunityPostResponse toPostResponse(CommunityPost post, boolean includeAnswers) {
        List<AnswerResponse> answers = includeAnswers
                ? answerRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(this::toAnswerResponse)
                        .toList()
                : List.of();
        return new CommunityPostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getCodeSnippet(),
                post.getImageUrl(),
                post.getLanguage(),
                post.getTags(),
                post.getCategory(),
                post.getStatus(),
                post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getCreatedAt(),
                answerRepository.countByPostId(post.getId()),
                answers
        );
    }

    private AnswerResponse toAnswerResponse(PostAnswer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getContent(),
                answer.getCodeSnippet(),
                answer.getAuthor().getFullName(),
                answer.getCreatedAt()
        );
    }

    private ChallengeResponse toChallengeResponse(DailyChallenge challenge) {
        return toChallengeResponse(challenge, challenge.getPublishDate());
    }

    private ChallengeResponse toChallengeResponse(DailyChallenge challenge, LocalDate displayDate) {
        List<SubmissionResponse> submissions = submissionRepository
                .findTop8ByChallengeIdOrderByCreatedAtDesc(challenge.getId()).stream()
                .map(this::toSubmissionResponse)
                .toList();
        return new ChallengeResponse(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getPrompt(),
                challenge.getDifficulty(),
                challenge.getLanguage(),
                challenge.getStarterCode(),
                challenge.getExpectedOutput(),
                challenge.getSolutionHint(),
                displayDate,
                submissionRepository.countByChallengeId(challenge.getId()),
                submissions
        );
    }

    private SubmissionResponse toSubmissionResponse(ChallengeSubmission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAnswerCode(),
                submission.getNotes(),
                submission.getAuthor().getFullName(),
                submission.getCorrect(),
                submission.getQualityScore(),
                submission.getExplanationScore(),
                submission.getSpeedRank(),
                submission.getSpeedBonus(),
                submission.getStreakBonus(),
                submission.getFinalScore(),
                submission.getReviewNote(),
                submission.getCreatedAt()
        );
    }

    private void applyAutomaticScore(ChallengeSubmission submission) {
        long correctCount = submissionRepository.countByChallengeIdAndCorrectTrue(submission.getChallenge().getId());
        int speedRank = (int) correctCount + 1;
        submission.setCorrect(true);
        submission.setQualityScore(0);
        submission.setExplanationScore(blankToNull(submission.getNotes()) == null ? 0 : EXPLANATION_BONUS);
        submission.setSpeedRank(speedRank);
        submission.setSpeedBonus(speedBonus(speedRank));
        submission.setStreakBonus(calculateStreakDays(submission.getAuthor().getId(), submission.getChallenge().getPublishDate()) * STREAK_DAILY_BONUS);
        applyFinalScore(submission);
    }

    private void applyFinalScore(ChallengeSubmission submission) {
        if (!Boolean.TRUE.equals(submission.getCorrect())) {
            submission.setFinalScore(0);
            return;
        }
        submission.setFinalScore(BASE_CORRECT_SCORE
                + safeScore(submission.getQualityScore())
                + safeScore(submission.getExplanationScore())
                + safeScore(submission.getSpeedBonus())
                + safeScore(submission.getStreakBonus()));
    }

    private int speedBonus(int speedRank) {
        if (speedRank == 1) {
            return 20;
        }
        if (speedRank == 2) {
            return 15;
        }
        if (speedRank == 3) {
            return 10;
        }
        return DAILY_SUBMIT_BONUS;
    }

    private int calculateStreakDays(Long userId, LocalDate currentChallengeDate) {
        List<LocalDate> solvedDates = new ArrayList<>(submissionRepository
                .findByAuthorIdAndCorrectTrueOrderByChallengePublishDateDescCreatedAtAsc(userId).stream()
                .map(submission -> submission.getChallenge().getPublishDate())
                .distinct()
                .toList());
        if (!solvedDates.contains(currentChallengeDate)) {
            solvedDates.add(currentChallengeDate);
        }
        solvedDates.sort(Comparator.reverseOrder());

        int streak = 0;
        LocalDate expectedDate = currentChallengeDate;
        for (LocalDate solvedDate : solvedDates) {
            if (solvedDate.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (solvedDate.isBefore(expectedDate)) {
                break;
            }
        }
        return streak;
    }

    private List<LeaderboardEntryResponse> buildLeaderboard(List<ChallengeSubmission> submissions) {
        Map<Long, LeaderboardAccumulator> users = new LinkedHashMap<>();
        for (ChallengeSubmission submission : submissions) {
            LeaderboardAccumulator current = users.computeIfAbsent(
                    submission.getAuthor().getId(),
                    id -> new LeaderboardAccumulator(submission.getAuthor().getId(), submission.getAuthor().getFullName())
            );
            current.add(submission);
        }
        return users.values().stream()
                .map(accumulator -> new LeaderboardEntryResponse(
                        accumulator.userId,
                        accumulator.fullName,
                        accumulator.totalScore,
                        accumulator.solvedCount,
                        accumulator.bestSpeedRank == Integer.MAX_VALUE ? 0 : accumulator.bestSpeedRank,
                        calculateCurrentStreakDays(accumulator.userId),
                        accumulator.latestSubmissionAt
                ))
                .sorted(Comparator
                        .comparing(LeaderboardEntryResponse::totalScore).reversed()
                        .thenComparing(LeaderboardEntryResponse::latestSubmissionAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    private int calculateCurrentStreakDays(Long userId) {
        List<LocalDate> solvedDates = submissionRepository
                .findByAuthorIdAndCorrectTrueOrderByChallengePublishDateDescCreatedAtAsc(userId).stream()
                .map(submission -> submission.getChallenge().getPublishDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        if (solvedDates.isEmpty()) {
            return 0;
        }
        int streak = 0;
        LocalDate expectedDate = solvedDates.get(0);
        for (LocalDate solvedDate : solvedDates) {
            if (solvedDate.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (solvedDate.isBefore(expectedDate)) {
                break;
            }
        }
        return streak;
    }

    private int safeScore(Integer value) {
        return value == null ? 0 : value;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private CommunityPost getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private void applyPostRequest(CommunityPost post, CommunityPostRequest request) {
        post.setTitle(request.title());
        post.setBody(request.body());
        post.setCodeSnippet(blankToNull(request.codeSnippet()));
        post.setImageUrl(blankToNull(request.imageUrl()));
        post.setLanguage(request.language());
        post.setTags(blankToNull(request.tags()));
        post.setCategory(request.category());
    }

    private void assertPostOwnerOrAdmin(CommunityPost post, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (post.getAuthor() == null || post.getAuthor().getId() == null || !post.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Khong co quyen sua hoac xoa bai dang nay");
        }
    }

    private void applyChallengeRequest(DailyChallenge challenge, DailyChallengeRequest request) {
        challenge.setTitle(request.title());
        challenge.setPrompt(request.prompt());
        challenge.setDifficulty(request.difficulty());
        challenge.setLanguage(request.language());
        challenge.setStarterCode(request.starterCode());
        challenge.setExpectedOutput(request.expectedOutput());
        challenge.setSolutionHint(blankToNull(request.solutionHint()));
        challenge.setPublishDate(request.publishDate());
    }
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static class LeaderboardAccumulator {
        private final Long userId;
        private final String fullName;
        private int totalScore;
        private long solvedCount;
        private int bestSpeedRank = Integer.MAX_VALUE;
        private LocalDateTime latestSubmissionAt;

        private LeaderboardAccumulator(Long userId, String fullName) {
            this.userId = userId;
            this.fullName = fullName;
        }

        private void add(ChallengeSubmission submission) {
            totalScore += submission.getFinalScore() == null ? 0 : submission.getFinalScore();
            solvedCount++;
            if (submission.getSpeedRank() != null && submission.getSpeedRank() > 0) {
                bestSpeedRank = Math.min(bestSpeedRank, submission.getSpeedRank());
            }
            if (latestSubmissionAt == null || submission.getCreatedAt().isAfter(latestSubmissionAt)) {
                latestSubmissionAt = submission.getCreatedAt();
            }
        }
    }
}
