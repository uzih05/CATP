package org.example.catp.service.strategy;

import org.example.catp.entity.Department;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RecommendationStrategy {
    /**
     * 사용자 점수와 학과 목록을 받아 추천 순위대로 정렬된 결과를 반환합니다.
     *
     * @param userScores 사용자 10개 적성 점수 (1.0 ~ 5.0)
     * @param userTags 사용자 관심사 태그 목록
     * @param departments 전체 학과 목록
     * @return 추천 결과 리스트 (점수 높은 순 정렬)
     */
    List<Map<String, Object>> recommend(List<Double> userScores, Set<String> userTags, List<Department> departments);
}