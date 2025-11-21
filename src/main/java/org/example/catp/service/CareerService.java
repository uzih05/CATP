package org.example.catp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Department;
import org.example.catp.entity.Question;
import org.example.catp.repository.DepartmentRepository;
import org.example.catp.repository.QuestionRepository;
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
    private final ObjectMapper objectMapper;

    // 10가지 적성 타입 (순서 중요 - DB에 저장된 배열 인덱스와 일치해야 함)
    private static final String[] APTITUDE_TYPES = {
            "언어능력", "논리/분석력", "창의력", "사회성/공감능력", "주도성/리더십",
            "신체-활동성", "예술감각/공간지각", "체계성/꼼꼼함", "탐구심", "문제해결능력"
    };

    /**
     * 사용자 답변을 분석하여 적성 점수와 관심사 태그를 추출합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyzeTest(List<Integer> answers) {
        // 1. 질문 데이터 가져오기
        List<Question> questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        if (questions.size() != answers.size()) {
            throw new IllegalArgumentException("답변 개수가 질문 개수와 일치하지 않습니다.");
        }

        // 2. 적성별 점수 계산 (10개 항목)
        double[] aptitudeScores = new double[10];
        int[] counts = new int[10];
        Set<String> interestTags = new HashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int answer = answers.get(i);

            // 역채점 처리 (1<->5, 2<->4, 3<->3)
            int score = q.isReverse() ? (6 - answer) : answer;

            // 적성 인덱스 찾기
            int typeIndex = getAptitudeIndex(q.getAptitudeType());
            if (typeIndex != -1) {
                aptitudeScores[typeIndex] += score;
                counts[typeIndex]++;
            }

            // 3. 관심사 태그 추출 (4점 이상인 답변에서만 추출)
            // "정성적 분석"을 위해 사용자가 긍정적으로 답한 문항의 키워드를 수집
            if (score >= 4) {
                try {
                    List<String> tags = objectMapper.readValue(q.getTags(), new TypeReference<>() {});
                    interestTags.addAll(tags);
                } catch (Exception e) {
                    log.warn("태그 파싱 실패: {}", e.getMessage());
                }
            }
        }

        // 평균 점수 계산 (1.0 ~ 5.0)
        List<Double> finalScores = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            double avg = counts[i] == 0 ? 0 : aptitudeScores[i] / counts[i];
            finalScores.add(Math.round(avg * 10) / 10.0);
        }

        // 4. 성향 분석 (가장 높은 점수의 적성 2가지)
        String personality = analyzePersonality(finalScores);

        // 5. 학과 매칭 (하이브리드 알고리즘 적용)
        List<Map<String, Object>> recommendedDepartments = recommendDepartments(finalScores, interestTags);

        // 상위 3개 (추천) / 하위 3개 (비추천) / 관심사 기반 (유사)
        List<Map<String, Object>> top3 = recommendedDepartments.stream().limit(3).collect(Collectors.toList());
        List<Map<String, Object>> worst3 = new ArrayList<>(recommendedDepartments.subList(Math.max(0, recommendedDepartments.size() - 3), recommendedDepartments.size()));
        Collections.reverse(worst3); // 뒤집어서 가장 안 맞는 순서로

        return Map.of(
                "scores", finalScores,
                "interest_tags", new ArrayList<>(interestTags),
                "personality", personality,
                "top_departments", top3,
                "worst_departments", worst3
        );
    }

    /**
     * [핵심 알고리즘] 학과 추천 로직
     * 1. 유클리드 거리 (기본 적성 매칭)
     * 2. 중요 적성 가중치 (학과의 핵심 역량이 부족하면 페널티)
     * 3. 태그 매칭 보너스 (관심사가 일치하면 가산점) -> 정확도 대폭 향상
     */
    private List<Map<String, Object>> recommendDepartments(List<Double> userScores, Set<String> userTags) {
        List<Department> allDepartments = departmentRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Department dept : allDepartments) {
            try {
                // 학과 점수 파싱 (1~10점 스케일)
                List<Integer> deptScores = objectMapper.readValue(dept.getAptitudeScores(), new TypeReference<>() {});
                List<String> deptTags = objectMapper.readValue(dept.getTags(), new TypeReference<>() {});

                double distanceSum = 0;
                double weightedPenalty = 0;

                // 1. 거리 계산 및 가중치 페널티 적용
                for (int i = 0; i < 10; i++) {
                    double userVal = userScores.get(i) * 2; // 5점 만점을 10점 만점으로 변환
                    double deptVal = deptScores.get(i);

                    // 기본 유클리드 거리
                    distanceSum += Math.pow(userVal - deptVal, 2);

                    // [가중치 로직] 학과에서 매우 중요하게 여기는 역량(8점 이상)인데 사용자가 낮으면 페널티 부여
                    if (deptVal >= 8 && userVal < 6) {
                        weightedPenalty += (deptVal - userVal) * 5; // 페널티 가중치
                    }
                }

                double baseDistance = Math.sqrt(distanceSum);
                // 최대 거리 (약 28.5) 기준으로 100점 환산
                double matchScore = Math.max(0, 100 - (baseDistance * 2.5) - weightedPenalty);

                // 2. 태그 매칭 보너스 (Tag Matching Bonus)
                // 사용자의 관심사와 학과 태그가 겹치면 점수 대폭 상승
                long matchingTagCount = userTags.stream()
                        .filter(deptTags::contains)
                        .count();

                // 태그 하나당 3점씩 보너스 (최대 15점)
                double tagBonus = Math.min(15, matchingTagCount * 3);

                double finalScore = Math.min(100, matchScore + tagBonus);

                // 결과 맵 생성
                Map<String, Object> map = new HashMap<>();
                map.put("department", dept);
                map.put("match_percentage", Math.round(finalScore * 10) / 10.0);
                map.put("reason", generateReason(userScores, deptScores, matchingTagCount));

                results.add(map);

            } catch (Exception e) {
                log.error("학과 매칭 중 오류: {}", dept.getName(), e);
            }
        }

        // 점수 높은 순 정렬
        results.sort((a, b) -> Double.compare((Double) b.get("match_percentage"), (Double) a.get("match_percentage")));

        return results;
    }

    private String generateReason(List<Double> userScores, List<Integer> deptScores, long tagMatchCount) {
        if (tagMatchCount >= 2) {
            return "관심 분야가 잘 맞고 적성도 일치합니다.";
        }
        // 강점 찾기 로직 등 추가 가능
        return "전반적인 적성 유형이 학과 인재상과 부합합니다.";
    }

    private int getAptitudeIndex(String type) {
        for (int i = 0; i < APTITUDE_TYPES.length; i++) {
            if (APTITUDE_TYPES[i].equals(type)) return i;
        }
        return -1;
    }

    private String analyzePersonality(List<Double> scores) {
        // 가장 높은 점수 인덱스 찾기
        int maxIndex = 0;
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(maxIndex)) {
                maxIndex = i;
            }
        }

        String[] types = {
                "언어형", "논리형", "창의형", "사회형", "리더형",
                "활동형", "예술형", "체계형", "탐구형", "실행형"
        };

        return types[maxIndex] + " 인재";
    }
}