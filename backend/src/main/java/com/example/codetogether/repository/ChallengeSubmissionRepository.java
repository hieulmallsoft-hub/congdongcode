package com.example.codetogether.repository;

import com.example.codetogether.entity.ChallengeSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Long> {
    List<ChallengeSubmission> findTop8ByChallengeIdOrderByCreatedAtDesc(Long challengeId);

    long countByChallengeId(Long challengeId);
}
