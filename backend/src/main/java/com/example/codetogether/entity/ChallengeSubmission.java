package com.example.codetogether.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_submissions")
public class ChallengeSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private DailyChallenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String answerCode;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private Boolean correct = true;

    @Column(nullable = false)
    private Integer qualityScore = 0;

    @Column(nullable = false)
    private Integer explanationScore = 0;

    @Column(nullable = false)
    private Integer speedRank = 0;

    @Column(nullable = false)
    private Integer speedBonus = 0;

    @Column(nullable = false)
    private Integer streakBonus = 0;

    @Column(nullable = false)
    private Integer finalScore = 0;

    @Column(columnDefinition = "text")
    private String reviewNote;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (correct == null) {
            correct = true;
        }
        if (qualityScore == null) {
            qualityScore = 0;
        }
        if (explanationScore == null) {
            explanationScore = 0;
        }
        if (speedRank == null) {
            speedRank = 0;
        }
        if (speedBonus == null) {
            speedBonus = 0;
        }
        if (streakBonus == null) {
            streakBonus = 0;
        }
        if (finalScore == null) {
            finalScore = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public DailyChallenge getChallenge() {
        return challenge;
    }

    public void setChallenge(DailyChallenge challenge) {
        this.challenge = challenge;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getAnswerCode() {
        return answerCode;
    }

    public void setAnswerCode(String answerCode) {
        this.answerCode = answerCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getExplanationScore() {
        return explanationScore;
    }

    public void setExplanationScore(Integer explanationScore) {
        this.explanationScore = explanationScore;
    }

    public Integer getSpeedRank() {
        return speedRank;
    }

    public void setSpeedRank(Integer speedRank) {
        this.speedRank = speedRank;
    }

    public Integer getSpeedBonus() {
        return speedBonus;
    }

    public void setSpeedBonus(Integer speedBonus) {
        this.speedBonus = speedBonus;
    }

    public Integer getStreakBonus() {
        return streakBonus;
    }

    public void setStreakBonus(Integer streakBonus) {
        this.streakBonus = streakBonus;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
