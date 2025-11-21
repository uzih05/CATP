package org.example.catp.service.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Department;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeightedDistanceStrategy implements RecommendationStrategy {

    private final ObjectMapper objectMapper;

    // 계열별 중요 역량 인덱스 (0:언어, 1:논리, 2:창의, 3:사회, 4:리더, 5:신체, 6:예술, 7:꼼꼼, 8:탐구, 9:문제해결)
    private static final Map<String, List<Integer>> CATEGORY_WEIGHTS = Map.of(
            "이공계", List.of(1, 8, 9),       // 논리, 탐구, 문제해결 중요
            "인문계", List.of(0, 3, 8),       // 언어, 사회성, 탐구 중요
            "경상계", List.of(1, 4, 7),       // 논리, 리더십, 꼼꼼함 중요
            "예체능", List.of(2, 6, 5),       // 창의, 예술, 신체 중요
            "보건의료", List.of(3, 7, 9),      // 사회성, 꼼꼼함, 문제해결 중요
            "교육계", List.of(0, 3, 4)        // 언어, 사회성, 리더십 중요
    );

    @Override
    public List<Map<String, Object>> recommend(List<Double> userScores, Set<String> userTags, List<Department> departments) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Department dept : departments) {
            try {
                // 학과 데이터 파싱
                List<Integer> deptScores = objectMapper.readValue(dept.getAptitudeScores(), new TypeReference<>() {});
                List<String> deptTags = objectMapper.readValue(dept.getTags(), new TypeReference<>() {});

                // 1. 적합도 점수 계산 (Weighted Distance Algorithm)
                double finalScore = calculateMatchScore(userScores, deptScores, dept.getCategory(), userTags, deptTags);

                // 2. 결과 맵핑
                Map<String, Object> map = new HashMap<>();
                map.put("department", dept);
                map.put("match_percentage", Math.round(finalScore * 10) / 10.0);

                // 태그 매칭 개수 계산 (사유 생성용)
                long matchingTagCount = userTags.stream().filter(deptTags::contains).count();
                map.put("reason", generateReason(matchingTagCount, dept.getCategory()));

                // 공통 태그 정보
                if (matchingTagCount > 0) {
                    map.put("common_tags", userTags.stream().filter(deptTags::contains).collect(Collectors.toList()));
                }

                results.add(map);

            } catch (Exception e) {
                log.error("학과 매칭 계산 실패: {}", dept.getName(), e);
            }
        }

        // 점수 높은 순 정렬
        results.sort((a, b) -> Double.compare((Double) b.get("match_percentage"), (Double) a.get("match_percentage")));
        return results;
    }

    private double calculateMatchScore(List<Double> userScores, List<Integer> deptScores, String category, Set<String> userTags, List<String> deptTags) {
        double totalPenalty = 0;

        // 해당 계열의 중요 역량 인덱스 가져오기 (없으면 빈 리스트)
        List<Integer> importantIndices = CATEGORY_WEIGHTS.getOrDefault(category, Collections.emptyList());

        for (int i = 0; i < 10; i++) {
            double userVal = userScores.get(i) * 2; // 10점 만점 환산
            double deptVal = deptScores.get(i);
            double diff = Math.abs(userVal - deptVal);

            // [가중치 로직]
            // 1. 학과에서 요구하는 핵심 역량(7점 이상)인데 사용자가 부족하면 -> 페널티 1.5배
            // 2. 계열별 중요 역량(importantIndices)인 경우 -> 페널티 1.2배 추가 강화
            double weight = 1.0;

            if (deptVal >= 7) {
                weight += 0.5;
            }
            if (importantIndices.contains(i)) {
                weight += 0.2;
            }

            // 3. 반대로 학과에서 중요하지 않은 역량(4점 이하)은 차이가 나도 관대하게 처리 -> 페널티 0.7배
            if (deptVal <= 4) {
                weight *= 0.7;
            }

            totalPenalty += diff * weight;
        }

        // 기본 점수 (100점 만점 기준, 페널티 차감 방식)
        double baseScore = Math.max(0, 100 - (totalPenalty * 1.1));

        // [태그 보너스] 관심사 일치 시 보너스 점수 (최대 15점)
        long matchCount = userTags.stream().filter(deptTags::contains).count();
        double tagBonus = Math.min(15, matchCount * 5);

        return Math.min(100, baseScore + tagBonus);
    }

    private String generateReason(long tagMatchCount, String category) {
        if (tagMatchCount >= 2) return "관심 분야가 잘 맞고, " + category + " 적성이 우수합니다.";
        if (tagMatchCount == 1) return "관심사가 일부 일치하며 적성이 부합합니다.";
        return category + " 계열로서 전반적인 적성 유형이 잘 맞습니다.";
    }
}