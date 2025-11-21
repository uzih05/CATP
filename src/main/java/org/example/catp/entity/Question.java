package org.example.catp.entity;

import com.fasterxml.jackson.annotation.JsonProperty; // ✅ 이 import 필수!
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @JsonProperty("text")
    private String questionText;

    @Column(nullable = false)
    @JsonProperty("aptitude_type") // 파이썬 스타일과 호환 유지
    private String aptitudeType;

    @JsonProperty("is_reverse") // 파이썬 스타일과 호환 유지
    private boolean isReverse;

    @Column(nullable = false)
    @JsonProperty("order")
    private Integer questionOrder;

    @Column(columnDefinition = "TEXT")
    private String tags;
}