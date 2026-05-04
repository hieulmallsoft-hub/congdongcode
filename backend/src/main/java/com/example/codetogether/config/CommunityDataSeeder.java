package com.example.codetogether.config;

import com.example.codetogether.entity.DailyChallenge;
import com.example.codetogether.repository.DailyChallengeRepository;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommunityDataSeeder implements CommandLineRunner {
    private final DailyChallengeRepository challengeRepository;

    public CommunityDataSeeder(DailyChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    @Override
    public void run(String... args) {
        if (challengeRepository.count() > 0) {
            return;
        }

        challengeRepository.save(challenge(
                "Dem tan suat phan tu",
                "Cho mot mang so nguyen, hay tra ve phan tu xuat hien nhieu nhat. Neu co nhieu dap an, tra ve phan tu nho nhat.",
                "Easy",
                "Java",
                "public int mostFrequent(int[] nums) {\n    // viet code tai day\n}",
                "Input: [4, 2, 4, 3, 2, 4] -> Output: 4",
                "Dung HashMap de dem so lan xuat hien, sau do so sanh tan suat va gia tri.",
                LocalDate.now()
        ));
        challengeRepository.save(challenge(
                "Kiem tra chuoi ngoac hop le",
                "Cho chuoi chi gom '(', ')', '[', ']', '{', '}', hay kiem tra chuoi co hop le khong.",
                "Medium",
                "JavaScript",
                "function isValid(s) {\n  // viet code tai day\n}",
                "Input: \"([]){}\" -> Output: true",
                "Dung stack, gap ngoac dong thi lay phan tu cuoi ra so khop.",
                LocalDate.now().plusDays(1)
        ));
        challengeRepository.save(challenge(
                "Duong di ngan nhat tren luoi",
                "Cho ma tran 0/1, 0 la o di duoc va 1 la vat can. Tim so buoc ngan nhat tu goc tren trai den goc duoi phai.",
                "Hard",
                "Java",
                "public int shortestPath(int[][] grid) {\n    // viet code tai day\n}",
                "Input: [[0,0,0],[1,1,0],[0,0,0]] -> Output: 4",
                "Dung BFS vi moi buoc co cung chi phi.",
                LocalDate.now().plusDays(2)
        ));
    }

    private DailyChallenge challenge(
            String title,
            String prompt,
            String difficulty,
            String language,
            String starterCode,
            String expectedOutput,
            String solutionHint,
            LocalDate publishDate
    ) {
        DailyChallenge challenge = new DailyChallenge();
        challenge.setTitle(title);
        challenge.setPrompt(prompt);
        challenge.setDifficulty(difficulty);
        challenge.setLanguage(language);
        challenge.setStarterCode(starterCode);
        challenge.setExpectedOutput(expectedOutput);
        challenge.setSolutionHint(solutionHint);
        challenge.setPublishDate(publishDate);
        return challenge;
    }
}
