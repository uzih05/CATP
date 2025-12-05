package org.example.catp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.AptitudeType;
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
    private final ScoreCalculator scoreCalculator;
    private final RecommendationStrategy recommendationStrategy;
    private final ObjectMapper objectMapper;

    /** 최종 출력할 관심사 태그 최대 개수 */
    private static final int MAX_INTEREST_TAGS = 10;

    // ========== 태그 우선순위 및 그룹화 ==========
    
    /** 대표 태그 (이 태그들이 우선 표시됨) */
    private static final Set<String> PRIMARY_TAGS = Set.of(
            // 분야별 대표 키워드
            "IT", "AI", "코딩", "데이터", "디자인", "경영", "금융", "법", "의료", "교육",
            "예술", "음악", "체육", "건축", "과학", "연구", "창업", "마케팅", "심리", "언어",
            "게임", "영상", "웹툰", "요리", "관광", "봉사", "리더십", "창의", "분석", "소통"
    );

    /** 유사 태그 그룹 (같은 그룹에서 1개만 선택) */
    private static final Map<String, List<String>> TAG_GROUPS = Map.ofEntries(
            // IT/개발 계열
            Map.entry("IT", List.of("IT", "코딩", "프로그래밍", "개발", "컴퓨터", "소프트웨어")),
            Map.entry("AI", List.of("AI", "인공지능", "머신러닝")),
            Map.entry("데이터", List.of("데이터", "빅데이터", "통계", "분석")),
            
            // 경영/경제 계열
            Map.entry("경영", List.of("경영", "비즈니스", "관리", "CEO")),
            Map.entry("금융", List.of("금융", "투자", "회계", "재테크")),
            Map.entry("창업", List.of("창업", "스타트업", "사업")),
            
            // 디자인/예술 계열
            Map.entry("디자인", List.of("디자인", "시각디자인", "산업디자인")),
            Map.entry("예술", List.of("예술", "미술", "창작")),
            Map.entry("음악", List.of("음악", "공연", "엔터테인먼트")),
            
            // 미디어/콘텐츠 계열
            Map.entry("영상", List.of("영상", "영화", "방송", "미디어")),
            Map.entry("게임", List.of("게임", "콘텐츠")),
            Map.entry("웹툰", List.of("웹툰", "만화")),
            
            // 언어/글쓰기 계열
            Map.entry("언어", List.of("언어", "외국어", "영어", "일본어", "중국어")),
            Map.entry("글쓰기", List.of("글쓰기", "문학", "작가", "스토리")),
            
            // 과학/공학 계열
            Map.entry("과학", List.of("과학", "연구", "실험")),
            Map.entry("공학", List.of("공학", "기계", "전기", "전자", "기술")),
            
            // 의료/보건 계열
            Map.entry("의료", List.of("의료", "간호", "건강", "보건")),
            Map.entry("심리", List.of("심리", "상담", "치료")),
            
            // 사회/봉사 계열
            Map.entry("봉사", List.of("봉사", "사회복지", "돌봄")),
            Map.entry("교육", List.of("교육", "교직", "교사")),
            
            // 행정/법 계열
            Map.entry("행정", List.of("행정", "공무원", "정책")),
            Map.entry("법", List.of("법", "법률", "정의"))
    );

    /**
     * 사용자 답변을 분석하여 적성 점수, 성향, 추천 학과 정보를 반환합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeTest(List<Integer> answers) {
        // 1. 질문 데이터 조회
        List<Question> questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        validateAnswers(questions.size(), answers.size());

        // 2. 적성 점수 계산
        List<Double> scores = scoreCalculator.calculate(questions, answers);

        // 3. 관심사 태그 추출 (원본)
        Map<String, Integer> rawTagCounts = extractRawInterestTags(questions, answers);
        
        // 4. 관심사 태그 필터링 (중복 제거 + 상위 N개)
        List<String> filteredTags = filterAndPrioritizeTags(rawTagCounts);

        // 5. 성향 분석
        String personality = analyzePersonality(scores);

        // 6. 학과 추천
        List<Department> allDepartments = departmentRepository.findAll();
        Set<String> tagSet = new HashSet<>(filteredTags);
        List<Map<String, Object>> recommendedDepartments = recommendationStrategy.recommend(scores, tagSet, allDepartments);

        // 7. 결과 가공
        Map<String, Object> result = buildResultMap(scores, filteredTags, personality, recommendedDepartments);

        // 8. Summary 생성
        Map<String, String> summary = generateSummary(scores, filteredTags, personality, recommendedDepartments);
        result.put("summary", summary);

        return result;
    }

    // ========== 내부 헬퍼 메서드 ==========

    private void validateAnswers(int questionSize, int answerSize) {
        if (questionSize != answerSize) {
            throw new IllegalArgumentException("답변 개수(" + answerSize + ")가 질문 개수(" + questionSize + ")와 일치하지 않습니다.");
        }
    }

    /**
     * 원본 태그와 빈도수 추출
     */
    private Map<String, Integer> extractRawInterestTags(List<Question> questions, List<Integer> answers) {
        Map<String, Integer> tagCounts = new HashMap<>();
        
        for (int i = 0; i < questions.size(); i++) {
            int answerValue = answers.get(i);
            
            // 4점 이상(긍정)이면 가중치 높게, 5점(매우 긍정)이면 더 높게
            if (answerValue >= 4) {
                int weight = (answerValue == 5) ? 2 : 1;
                
                try {
                    List<String> questionTags = objectMapper.readValue(
                            questions.get(i).getTags(), 
                            new TypeReference<>() {}
                    );
                    
                    for (String tag : questionTags) {
                        tagCounts.merge(tag, weight, Integer::sum);
                    }
                } catch (Exception e) {
                    log.warn("태그 파싱 실패 (질문 ID: {}): {}", questions.get(i).getId(), e.getMessage());
                }
            }
        }
        
        return tagCounts;
    }

    /**
     * 태그 필터링 및 우선순위 정렬
     * 1. 유사 태그 그룹에서 대표 태그만 선택
     * 2. 빈도수 높은 순 정렬
     * 3. 대표 태그 우선
     * 4. 최대 MAX_INTEREST_TAGS개만 반환
     */
    private List<String> filterAndPrioritizeTags(Map<String, Integer> rawTagCounts) {
        if (rawTagCounts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> selectedTags = new LinkedHashSet<>();
        Set<String> usedGroups = new HashSet<>();

        // 1. 빈도수 기준 정렬
        List<Map.Entry<String, Integer>> sortedTags = rawTagCounts.entrySet().stream()
                .sorted((a, b) -> {
                    // 빈도수 높은 순
                    int countCompare = b.getValue().compareTo(a.getValue());
                    if (countCompare != 0) return countCompare;
                    
                    // 동점이면 대표 태그 우선
                    boolean aIsPrimary = PRIMARY_TAGS.contains(a.getKey());
                    boolean bIsPrimary = PRIMARY_TAGS.contains(b.getKey());
                    return Boolean.compare(bIsPrimary, aIsPrimary);
                })
                .collect(Collectors.toList());

        // 2. 그룹별 대표 태그만 선택
        for (Map.Entry<String, Integer> entry : sortedTags) {
            String tag = entry.getKey();
            
            // 이 태그가 속한 그룹 찾기
            String belongingGroup = findBelongingGroup(tag);
            
            if (belongingGroup != null) {
                // 이미 같은 그룹의 태그가 선택되었으면 스킵
                if (usedGroups.contains(belongingGroup)) {
                    continue;
                }
                usedGroups.add(belongingGroup);
                
                // 그룹의 대표 태그 사용 (그룹명이 대표 태그)
                selectedTags.add(belongingGroup);
            } else {
                // 그룹에 속하지 않는 태그는 그대로 추가
                selectedTags.add(tag);
            }

            if (selectedTags.size() >= MAX_INTEREST_TAGS) {
                break;
            }
        }

        return new ArrayList<>(selectedTags);
    }

    /**
     * 태그가 속한 그룹 찾기
     */
    private String findBelongingGroup(String tag) {
        for (Map.Entry<String, List<String>> group : TAG_GROUPS.entrySet()) {
            if (group.getValue().contains(tag)) {
                return group.getKey();
            }
        }
        return null;
    }

    private String analyzePersonality(List<Double> scores) {
        int maxIndex = 0;
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(maxIndex)) {
                maxIndex = i;
            }
        }
        
        AptitudeType dominantType = AptitudeType.fromIndex(maxIndex);
        return dominantType.getPersonalityType() + " 인재";
    }

    /**
     * 결과 요약 문구 생성
     */
    private Map<String, String> generateSummary(
            List<Double> scores,
            List<String> interestTags,
            String personality,
            List<Map<String, Object>> recommendedDepartments
    ) {
        Map<String, String> summary = new HashMap<>();

        // 1. 성향 요약
        summary.put("personality", personality + " 유형입니다.");

        // 2. 강점 분석
        List<AptitudeType> topAptitudes = findTopAptitudes(scores, 3);
        String strengthText = topAptitudes.stream()
                .map(AptitudeType::getDisplayName)
                .collect(Collectors.joining(", "));
        summary.put("strength", strengthText + " 분야에서 강점을 보입니다.");

        // 3. 관심사 요약 (필터링된 태그 사용)
        if (!interestTags.isEmpty()) {
            String interestText = String.join(", ", interestTags.subList(0, Math.min(5, interestTags.size())));
            if (interestTags.size() > 5) {
                interestText += " 등";
            }
            summary.put("interest", interestText + "에 관심이 있습니다.");
        }

        // 4. 1순위 학과 요약
        if (!recommendedDepartments.isEmpty()) {
            Map<String, Object> topDept = recommendedDepartments.get(0);
            Department dept = (Department) topDept.get("department");
            Double matchPercentage = (Double) topDept.get("match_percentage");
            String reason = (String) topDept.get("reason");
            
            summary.put("top_department", String.format(
                    "%s이(가) %.1f%% 일치합니다. %s",
                    dept.getName(), matchPercentage, reason
            ));
        }

        return summary;
    }

    private List<AptitudeType> findTopAptitudes(List<Double> scores, int n) {
        List<Map.Entry<Integer, Double>> indexed = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            indexed.add(Map.entry(i, scores.get(i)));
        }
        
        return indexed.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(entry -> AptitudeType.fromIndex(entry.getKey()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildResultMap(
            List<Double> scores,
            List<String> interestTags,
            String personality,
            List<Map<String, Object>> recommendedDepartments
    ) {
        // 상위 3개 (추천)
        List<Map<String, Object>> top3 = recommendedDepartments.stream()
                .limit(3)
                .collect(Collectors.toList());

        // 하위 3개 (비추천)
        List<Map<String, Object>> worst3 = new ArrayList<>(
                recommendedDepartments.subList(
                        Math.max(0, recommendedDepartments.size() - 3),
                        recommendedDepartments.size()
                )
        );
        Collections.reverse(worst3);

        // 관심사 기반 유사 학과
        List<Map<String, Object>> similar = recommendedDepartments.stream()
                .filter(d -> !top3.contains(d))
                .filter(d -> {
                    String reason = (String) d.get("reason");
                    return reason != null && reason.contains("관심 분야");
                })
                .limit(3)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("scores", scores);
        result.put("interest_tags", interestTags); // 필터링된 태그
        result.put("personality", personality);
        result.put("top_departments", top3);
        result.put("worst_departments", worst3);
        result.put("similar_departments", similar);

        return result;
    }
}
