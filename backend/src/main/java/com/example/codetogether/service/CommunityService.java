package com.example.codetogether.service;

import com.example.codetogether.dto.AnswerRequest;
import com.example.codetogether.dto.AnswerResponse;
import com.example.codetogether.dto.ChallengeResponse;
import com.example.codetogether.dto.CommunityPostRequest;
import com.example.codetogether.dto.CommunityPostResponse;
import com.example.codetogether.dto.SubmissionRequest;
import com.example.codetogether.dto.SubmissionResponse;
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
        post.setTitle(request.title());
        post.setBody(request.body());
        post.setCodeSnippet(blankToNull(request.codeSnippet()));
        post.setLanguage(request.language());
        post.setTags(blankToNull(request.tags()));
        post.setCategory(request.category());
        post.setAuthor(user);
        return toPostResponse(postRepository.save(post), false);
    }

    @Transactional
    public AnswerResponse addAnswer(Long userId, Long postId, AnswerRequest request) {
        User user = getUser(userId);
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Post not found"));

        PostAnswer answer = new PostAnswer();
        answer.setPost(post);
        answer.setAuthor(user);
        answer.setContent(request.content());
        answer.setCodeSnippet(blankToNull(request.codeSnippet()));
        return toAnswerResponse(answerRepository.save(answer));
    }

    @Transactional(readOnly = true)
    public ChallengeResponse getDailyChallenge() {
        DailyChallenge challenge = challengeRepository
                .findFirstByPublishDateLessThanEqualOrderByPublishDateDesc(LocalDate.now())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No daily challenge yet"));
        return toChallengeResponse(challenge);
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
                post.getLanguage(),
                post.getTags(),
                post.getCategory(),
                post.getStatus(),
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
                challenge.getPublishDate(),
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
