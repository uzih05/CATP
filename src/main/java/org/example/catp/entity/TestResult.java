package org.example.catp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
public class TestResult {

    @Id
    @Column(length = 20)
    private String id; // 공유용 랜덤 ID (직접 생성해서 넣음)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userAnswers; // 사용자가 선택한 답변 (JSON)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userScores; // 계산된 10개 적성 점수 (JSON)

    @Column(columnDefinition = "TEXT")
    private String interestTags; // 도출된 관심사 태그 (JSON)

    private String personalityType; // 성향 유형 (예: "논리형")

    @Column(columnDefinition = "TEXT")
    private String topDepartments; // 추천 학과 Top 3 결과 (JSON)

    @Column(columnDefinition = "TEXT")
    private String worstDepartments; // 비추천 학과 결과 (JSON)

    @Column(columnDefinition = "TEXT")
    private String similarDepartments; // 관심사 기반 추천 학과 (JSON)

    @CreationTimestamp
    private LocalDateTime createdAt; // 생성 시간 자동 기록
}