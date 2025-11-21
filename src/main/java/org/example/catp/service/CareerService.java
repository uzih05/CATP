package org.example.catp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Department;
import org.example.catp.entity.Question;
import org.example.catp.repository.DepartmentRepository;
import org.example.catp.repository.QuestionRepository;
import org.example.catp.service.calculator.ScoreCalculator;
import org.example.catp.service.strategy.RecommendationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareerService {

    private final QuestionRepository questionRepository;
    private final DepartmentRepository departmentRepository;
    private final ScoreCalculator scoreCalculator;          // 점수 계산 전문가
    private final RecommendationStrategy recommendationStrategy; // 추천 전략 전문가
    private final ObjectMapper objectMapper;

    /**
     * 사용자 답변을 분석하여 적성 점수, 성향, 추천 학과 정보를 반환합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeTest(List<Integer> answers) {
        // 1. 질문 데이터 조회
        List<Question> questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        validateAnswers(questions.size(), answers.size());

        // 2. 적성 점수 계산 (ScoreCalculator 위임)
        List<Double> scores = scoreCalculator.calculate(questions, answers);

        // 3. 관심사 태그 추출 (Service 내부 로직 유지)
        Set<String> interestTags = extractInterestTags(questions, answers);

        // 4. 성향 분석 (Service 내부 로직 유지)
        String personality = analyzePersonality(scores);

        // 5. 학과 추천 (RecommendationStrategy 위임)
        List<Department> allDepartments = departmentRepository.findAll();
        List<Map<String, Object>> recommendedDepartments = recommendationStrategy.recommend(scores, interestTags, allDepartments);

        // 6. 결과 가공 (상위/하위/유사 학과 분리)
        return buildResultMap(scores, interestTags, personality, recommendedDepartments);
    }

    // --- 내부 헬퍼 메서드 ---

    private void validateAnswers(int questionSize, int answerSize) {
        if (questionSize != answerSize) {
            throw new IllegalArgumentException("답변 개수(" + answerSize + ")가 질문 개수(" + questionSize + ")와 일치하지 않습니다.");
        }
    }

    private Set<String> extractInterestTags(List<Question> questions, List<Integer> answers) {
        Set<String> tags = new HashSet<>();
        for (int i = 0; i < questions.size(); i++) {
            // 4점 이상(긍정 응답)인 경우에만 태그 수집
            if (answers.get(i) >= 4) {
                try {
                    List<String> questionTags = objectMapper.readValue(questions.get(i).getTags(), new TypeReference<>() {});
                    tags.addAll(questionTags);
                } catch (Exception e) {
                    log.warn("태그 파싱 실패 (질문 ID: {}): {}", questions.get(i).getId(), e.getMessage());
                }
            }
        }
        return tags;
    }

    private String analyzePersonality(List<Double> scores) {
        String[] types = {
                "언어형", "논리형", "창의형", "사회형", "리더형",
                "활동형", "예술형", "체계형", "탐구형", "실행형"
        };

        // 가장 점수가 높은 적성 찾기
        int maxIndex = 0;
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(maxIndex)) {
                maxIndex = i;
            }
        }
        return types[maxIndex] + " 인재";
    }

    private Map<String, Object> buildResultMap(List<Double> scores, Set<String> interestTags, String personality, List<Map<String, Object>> recommendedDepartments) {
        // 상위 3개 (추천)
        List<Map<String, Object>> top3 = recommendedDepartments.stream()
                .limit(3)
                .collect(Collectors.toList());

        // 하위 3개 (비추천 - 뒤에서 3개)
        List<Map<String, Object>> worst3 = new ArrayList<>(
                recommendedDepartments.subList(Math.max(0, recommendedDepartments.size() - 3), recommendedDepartments.size())
        );
        Collections.reverse(worst3);

        // 관심사 기반 유사 학과 (상위 3개 제외 & '관심 분야' 키워드가 포함된 학과)
        List<Map<String, Object>> similar = recommendedDepartments.stream()
                .filter(d -> !top3.contains(d))
                .filter(d -> {
                    String reason = (String) d.get("reason");
                    return reason != null && reason.contains("관심 분야가 잘 맞고");
                })
                .limit(3)
                .collect(Collectors.toList());

        return Map.of(
                "scores", scores,
                "interest_tags", new ArrayList<>(interestTags),
                "personality", personality,
                "top_departments", top3,
                "worst_departments", worst3,
                "similar_departments", similar
        );
    }
}