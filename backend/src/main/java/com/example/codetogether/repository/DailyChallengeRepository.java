package com.example.codetogether.repository;

import com.example.codetogether.entity.DailyChallenge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {
    Optional<DailyChallenge> findFirstByPublishDateLessThanEqualOrderByPublishDateDesc(LocalDate publishDate);

    Optional<DailyChallenge> findByPublishDate(LocalDate publishDate);

    List<DailyChallenge> findAllByOrderByPublishDateDesc();
}
