package org.example.catp.service.calculator;

import org.example.catp.entity.Question;
import java.util.List;

public interface ScoreCalculator {
    /**
     * 질문 목록과 사용자 답변을 받아 10가지 적성 점수 리스트를 반환합니다.
     * 반환 순서는 APTITUDE_TYPES 순서와 일치해야 합니다.
     *
     * @param questions 질문 엔티티 리스트 (정렬됨)
     * @param answers 사용자가 선택한 답변 리스트 (1~5)
     * @return 계산된 10개 적성 점수 (1.0 ~ 5.0)
     */
    List<Double> calculate(List<Question> questions, List<Integer> answers);
}