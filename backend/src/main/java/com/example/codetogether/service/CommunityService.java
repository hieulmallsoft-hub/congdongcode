package com.example.codetogether.service;

import com.example.codetogether.dto.request.AnswerRequest;
import com.example.codetogether.dto.request.CommunityPostRequest;
import com.example.codetogether.dto.request.DailyChallengeRequest;
import com.example.codetogether.dto.request.SubmissionRequest;
import com.example.codetogether.dto.response.AnswerResponse;
import com.example.codetogether.dto.response.ChallengeResponse;
import com.example.codetogether.dto.response.CommunityPostResponse;
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
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {
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

        ChallengeSubmission submission = new ChallengeSubmission();
        submission.setChallenge(challenge);
        submission.setAuthor(user);
        submission.setAnswerCode(request.answerCode());
        submission.setNotes(blankToNull(request.notes()));
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
                submission.getCreatedAt()
        );
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
}
