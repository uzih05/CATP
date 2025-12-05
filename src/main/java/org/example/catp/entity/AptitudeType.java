package org.example.catp.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 10가지 적성 유형을 정의하는 Enum
 * 배열 인덱스 하드코딩 대신 이 Enum을 사용하여 타입 안전성을 확보합니다.
 */
@Getter
@RequiredArgsConstructor
public enum AptitudeType {

    LANGUAGE(0, "언어능력", "언어형"),
    LOGIC(1, "논리/분석력", "논리형"),
    CREATIVITY(2, "창의력", "창의형"),
    SOCIAL(3, "사회성/공감능력", "사회형"),
    LEADERSHIP(4, "주도성/리더십", "리더형"),
    PHYSICAL(5, "신체-활동성", "활동형"),
    ARTISTIC(6, "예술감각/공간지각", "예술형"),
    SYSTEMATIC(7, "체계성/꼼꼼함", "체계형"),
    INQUIRY(8, "탐구심", "탐구형"),
    PROBLEM_SOLVING(9, "문제해결능력", "실행형");

    private final int index;
    private final String displayName;
    private final String personalityType;

    /**
     * 인덱스로 AptitudeType 찾기
     */
    public static AptitudeType fromIndex(int index) {
        for (AptitudeType type : values()) {
            if (type.index == index) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 적성 인덱스: " + index);
    }

    /**
     * displayName으로 AptitudeType 찾기 (DB 데이터 매핑용)
     */
    public static AptitudeType fromDisplayName(String displayName) {
        for (AptitudeType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 적성 타입명: " + displayName);
    }

    /**
     * 모든 displayName을 배열로 반환 (기존 APTITUDE_TYPES 배열 대체용)
     */
    public static String[] getAllDisplayNames() {
        String[] names = new String[values().length];
        for (AptitudeType type : values()) {
            names[type.index] = type.displayName;
        }
        return names;
    }

    /**
     * 모든 personalityType을 배열로 반환
     */
    public static String[] getAllPersonalityTypes() {
        String[] types = new String[values().length];
        for (AptitudeType type : values()) {
            types[type.index] = type.personalityType;
        }
        return types;
    }
}
