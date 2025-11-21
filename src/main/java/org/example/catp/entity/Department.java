package org.example.catp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aptitudeScores; // JSON 문자열 (예: "[8, 6, 7...]")

    @Column(columnDefinition = "TEXT")
    private String description;

    private String url;

    @Column(columnDefinition = "TEXT")
    private String tags; // JSON 문자열

    private String category; // 계열 (예: "이공계")
}