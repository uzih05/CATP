package org.example.catp.service.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.AptitudeType;
import org.example.catp.entity.Department;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.catp.entity.AptitudeType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeightedDistanceStrategy implements RecommendationStrategy {

    private final ObjectMapper objectMapper;

    // ========== 상수 정의 ==========
    
    /** 적성 점수 배점 (100점 만점 중) */
    private static final double APTITUDE_MAX_SCORE = 70.0;
    
    /** 흥미 태그 배점 (100점 만점 중) */
    private static final double INTEREST_MAX_SCORE = 30.0;
    
    /** 태그 1개당 보너스 점수 */
    private static final double TAG_BONUS_PER_MATCH = 10.0;
    
    /** 과락 기준: 학과 요구 점수 */
    private static final int CRITICAL_DEPT_THRESHOLD = 8;
    
    /** 과락 기준: 사용자 점수 (10점 만점 환산) */
    private static final double CRITICAL_USER_THRESHOLD = 5.0;

    // ========== 계열별 중요 역량 ==========
    
    private static final Map<String, List<AptitudeType>> CATEGORY_WEIGHTS = Map.ofEntries(
            Map.entry("이공계", List.of(LOGIC, INQUIRY, PROBLEM_SOLVING)),
            Map.entry("인문계", List.of(LANGUAGE, SOCIAL, INQUIRY)),
            Map.entry("경상계", List.of(LOGIC, LEADERSHIP, SYSTEMATIC)),
            Map.entry("예체능", List.of(CREATIVITY, ARTISTIC, PHYSICAL)),
            Map.entry("보건의료", List.of(SOCIAL, SYSTEMATIC, PROBLEM_SOLVING)),
            Map.entry("교육계", List.of(LANGUAGE, SOCIAL, LEADERSHIP)),
            Map.entry("사회과학", List.of(LOGIC, SOCIAL, SYSTEMATIC)),
            Map.entry("관광·서비스", List.of(SOCIAL, LANGUAGE, CREATIVITY)),
            Map.entry("안전·기술", List.of(PHYSICAL, SYSTEMATIC, PROBLEM_SOLVING)),
            Map.entry("융합·미래", List.of(CREATIVITY, LOGIC, LEADERSHIP)),
            Map.entry("기타", List.of(LOGIC, SOCIAL, PROBLEM_SOLVING))
    );

    @Override
    public List<Map<String, Object>> recommend(List<Double> userScores, Set<String> userTags, List<Department> departments) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Department dept : departments) {
            try {
                List<Integer> deptScores = objectMapper.readValue(dept.getAptitudeScores(), new TypeReference<>() {});
                List<String> deptTags = objectMapper.readValue(dept.getTags(), new TypeReference<>() {});

                MatchResult matchResult = calculateMatchScore(userScores, deptScores, dept.getCategory(), userTags, deptTags);

                Map<String, Object> map = new HashMap<>();
                map.put("department", dept);
                map.put("match_percentage", Math.round(matchResult.score * 10) / 10.0);
                map.put("reason", matchResult.reason);
                
                if (matchResult.hasCriticalFail) {
                    map.put("mismatch_reason", matchResult.criticalFailReason);
                }

                if (matchResult.matchingTagCount > 0) {
                    map.put("common_tags", userTags.stream()
                            .filter(deptTags::contains)
                            .limit(5) // 공통 태그도 최대 5개만
                            .collect(Collectors.toList()));
                }

                results.add(map);

            } catch (Exception e) {
                log.error("학과 매칭 계산 실패: {}", dept.getName(), e);
            }
        }

        results.sort((a, b) -> Double.compare(
                (Double) b.get("match_percentage"), 
                (Double) a.get("match_percentage")
        ));
        
        return results;
    }

    /**
     * Cosine Similarity 기반 매칭 점수 계산
     */
    private MatchResult calculateMatchScore(
            List<Double> userScores, 
            List<Integer> deptScores, 
            String category,
            Set<String> userTags, 
            List<String> deptTags
    ) {
        // 1. 사용자 점수를 10점 만점으로 환산
        double[] userVector = new double[10];
        double[] deptVector = new double[10];
        
        for (int i = 0; i < 10; i++) {
            userVector[i] = userScores.get(i) * 2; // 5점 → 10점 만점
            deptVector[i] = deptScores.get(i);
        }

        // 2. 과락 체크
        boolean hasCriticalFail = false;
        String criticalFailReason = null;
        List<String> weakPoints = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            if (deptVector[i] >= CRITICAL_DEPT_THRESHOLD && userVector[i] < CRITICAL_USER_THRESHOLD) {
                hasCriticalFail = true;
                AptitudeType aptitude = AptitudeType.fromIndex(i);
                criticalFailReason = String.format(
                        "%s 역량이 부족합니다 (요구: %.0f점, 보유: %.1f점)",
                        aptitude.getDisplayName(), deptVector[i], userVector[i]
                );
                weakPoints.add(aptitude.getDisplayName());
            }
        }

        // 3. Cosine Similarity 계산
        double cosineSimilarity = calculateCosineSimilarity(userVector, deptVector);
        
        // 4. 가중치 적용된 Cosine Similarity (계열별 중요 역량 반영)
        double weightedSimilarity = calculateWeightedCosineSimilarity(userVector, deptVector, category);
        
        // 5. 두 유사도의 조합 (기본 70% + 가중치 30%)
        double combinedSimilarity = (cosineSimilarity * 0.7) + (weightedSimilarity * 0.3);
        
        // 6. 적성 점수 (70점 만점)
        double aptitudeScore = combinedSimilarity * APTITUDE_MAX_SCORE;
        
        // 과락 시 감점
        if (hasCriticalFail) {
            aptitudeScore *= 0.6;
        }

        // 7. 흥미 점수 (30점 만점)
        long matchingTagCount = userTags.stream().filter(deptTags::contains).count();
        double interestScore = Math.min(INTEREST_MAX_SCORE, matchingTagCount * TAG_BONUS_PER_MATCH);

        // 8. 최종 점수
        double finalScore = aptitudeScore + interestScore;

        // 9. 강점 분석
        List<String> strongPoints = findStrongPoints(userVector, deptVector);

        // 10. 추천 사유 생성
        String reason = generateReason(matchingTagCount, category, strongPoints, hasCriticalFail, cosineSimilarity);

        return new MatchResult(finalScore, reason, hasCriticalFail, criticalFailReason, matchingTagCount);
    }

    /**
     * Cosine Similarity 계산
     * 결과: 0.0 ~ 1.0 (1에 가까울수록 유사)
     */
    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            magnitudeA += vectorA[i] * vectorA[i];
            magnitudeB += vectorB[i] * vectorB[i];
        }

        magnitudeA = Math.sqrt(magnitudeA);
        magnitudeB = Math.sqrt(magnitudeB);

        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        return dotProduct / (magnitudeA * magnitudeB);
    }

    /**
     * 계열별 중요 역량에 가중치를 적용한 Cosine Similarity
     */
    private double calculateWeightedCosineSimilarity(double[] userVector, double[] deptVector, String category) {
        List<AptitudeType> importantTypes = CATEGORY_WEIGHTS.getOrDefault(category, Collections.emptyList());
        Set<Integer> importantIndices = importantTypes.stream()
                .map(AptitudeType::getIndex)
                .collect(Collectors.toSet());

        double[] weightedUser = new double[10];
        double[] weightedDept = new double[10];

        for (int i = 0; i < 10; i++) {
            double weight = importantIndices.contains(i) ? 1.5 : 1.0;
            weightedUser[i] = userVector[i] * weight;
            weightedDept[i] = deptVector[i] * weight;
        }

        return calculateCosineSimilarity(weightedUser, weightedDept);
    }

    /**
     * 사용자가 학과 요구치 이상인 강점 역량 찾기
     */
    private List<String> findStrongPoints(double[] userVector, double[] deptVector) {
        List<String> strongPoints = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            // 학과가 7점 이상 요구하고, 사용자가 그 이상인 경우
            if (deptVector[i] >= 7 && userVector[i] >= deptVector[i]) {
                strongPoints.add(AptitudeType.fromIndex(i).getDisplayName());
            }
        }
        
        return strongPoints;
    }

    /**
     * 추천 사유 생성
     */
    private String generateReason(long tagMatchCount, String category, List<String> strongPoints, 
                                   boolean hasCriticalFail, double similarity) {
        
        if (hasCriticalFail) {
            return category + " 계열이지만, 일부 핵심 역량 보완이 필요합니다.";
        }

        // 높은 유사도 + 태그 매칭
        if (similarity >= 0.95 && tagMatchCount >= 2) {
            return "관심 분야와 적성이 모두 뛰어나게 일치합니다!";
        }

        // 강점이 있고 태그도 맞음
        if (strongPoints.size() >= 2 && tagMatchCount >= 2) {
            String strengths = String.join(", ", strongPoints.subList(0, Math.min(2, strongPoints.size())));
            return "관심 분야가 잘 맞고, " + strengths + " 역량이 뛰어납니다.";
        }

        // 강점만 있음
        if (strongPoints.size() >= 2) {
            String strengths = String.join(", ", strongPoints.subList(0, Math.min(2, strongPoints.size())));
            return strengths + " 등 핵심 역량을 갖추고 있습니다.";
        }

        // 태그만 맞음
        if (tagMatchCount >= 2) {
            return "관심 분야가 잘 맞고, " + category + " 적성이 우수합니다.";
        }

        if (tagMatchCount == 1) {
            return "관심사가 일부 일치하며 적성이 부합합니다.";
        }

        // 유사도 기반 기본 메시지
        if (similarity >= 0.9) {
            return category + " 계열로서 적성이 매우 잘 맞습니다.";
        } else if (similarity >= 0.8) {
            return category + " 계열로서 전반적인 적성 유형이 잘 맞습니다.";
        }
        
        return category + " 계열과 적성이 어느 정도 부합합니다.";
    }

    /**
     * 매칭 결과 내부 클래스
     */
    private static class MatchResult {
        final double score;
        final String reason;
        final boolean hasCriticalFail;
        final String criticalFailReason;
        final long matchingTagCount;

        MatchResult(double score, String reason, boolean hasCriticalFail, String criticalFailReason, long matchingTagCount) {
            this.score = score;
            this.reason = reason;
            this.hasCriticalFail = hasCriticalFail;
            this.criticalFailReason = criticalFailReason;
            this.matchingTagCount = matchingTagCount;
        }
    }
}
