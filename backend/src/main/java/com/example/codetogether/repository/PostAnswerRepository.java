package com.example.codetogether.repository;

import com.example.codetogether.entity.PostAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAnswerRepository extends JpaRepository<PostAnswer, Long> {
    List<PostAnswer> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);
}
