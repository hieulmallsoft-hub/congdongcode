package com.example.codetogether.repository;

import com.example.codetogether.entity.ChallengeSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Long> {
    List<ChallengeSubmission> findTop8ByChallengeIdOrderByCreatedAtDesc(Long challengeId);

    List<ChallengeSubmission> findByChallengeIdOrderByFinalScoreDescCreatedAtAsc(Long challengeId);

    List<ChallengeSubmission> findByAuthorIdAndCorrectTrueOrderByChallengePublishDateDescCreatedAtAsc(Long authorId);

    List<ChallengeSubmission> findByCorrectTrueOrderByFinalScoreDescCreatedAtAsc();

    long countByChallengeId(Long challengeId);

    long countByChallengeIdAndCorrectTrue(Long challengeId);

    boolean existsByChallengeIdAndAuthorId(Long challengeId, Long authorId);
}
