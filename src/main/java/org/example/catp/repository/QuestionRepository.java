package org.example.catp.repository;

import org.example.catp.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 질문 순서(questionOrder)대로 오름차순 정렬하여 모두 조회
    List<Question> findAllByOrderByQuestionOrderAsc();
}