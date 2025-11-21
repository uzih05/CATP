package org.example.catp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catp.entity.Department;

import java.util.*;

@Data
@NoArgsConstructor
public class DepartmentImportDto {

    @JsonProperty("학과")
    private String name;

    @JsonProperty("적성")
    private List<String> aptitudeDescription;

    @JsonProperty("URL")
    private String url;

    @JsonProperty("적성점수")
    private List<Integer> aptitudeScores;

    /**
     * DTO 데이터를 기반으로 DB에 저장할 Department 엔티티를 생성하여 반환합니다.
     * (태그 추출 및 카테고리 분류 로직 포함)
     */
    public Department toEntity(ObjectMapper objectMapper) {
        Department dept = new Department();
        dept.setName(this.name);
        dept.setUrl(this.url);
        dept.setCategory(inferCategory(this.name));

        try {
            // 리스트 데이터를 JSON 문자열로 변환하여 저장
            dept.setAptitudeScores(objectMapper.writeValueAsString(this.aptitudeScores));
            dept.setDescription(objectMapper.writeValueAsString(this.aptitudeDescription));

            // 태그 추출 로직 수행
            List<String> extractedTags = extractTags(this.aptitudeDescription);
            dept.setTags(objectMapper.writeValueAsString(extractedTags));

        } catch (Exception e) {
            throw new RuntimeException("학과 데이터 변환 중 오류 발생: " + this.name, e);
        }

        return dept;
    }

    // --- 아래는 기존 DataLoader에서 옮겨온 로직들입니다 ---

    private String inferCategory(String name) {
        if (containsAny(name, "공학", "컴퓨터", "전기", "기계", "건축", "토목", "화학", "소재", "신소재", "데이터", "인공지능", "소프트웨어")) return "이공계";
        if (containsAny(name, "경영", "경제", "금융", "회계", "무역", "부동산", "물류", "IT금융", "창업")) return "경상계";
        if (containsAny(name, "국어", "영어", "일본", "중국", "한국어", "문학", "역사", "한문")) return "인문계";
        if (containsAny(name, "디자인", "예술", "미술", "체육", "음악", "공연", "영화", "게임", "웹툰", "산업디자인", "시각디자인", "생활체육", "축구", "태권도")) return "예체능";
        if (containsAny(name, "간호", "물리치료", "작업치료", "방사선", "보건", "식품영양", "재활", "운동처방", "동물보건")) return "보건의료";
        if (containsAny(name, "교육과", "사범")) return "교육계";
        if (containsAny(name, "법학", "행정", "경찰", "사회복지", "상담", "문헌정보")) return "사회과학";
        if (containsAny(name, "관광", "호텔", "외식", "조리", "패션", "한식")) return "관광·서비스";
        if (containsAny(name, "소방", "자동차")) return "안전·기술";
        if (containsAny(name, "미네르바", "로컬벤처", "농식품", "반려동물", "자유전공", "펫산업")) return "융합·미래";
        return "기타";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private List<String> extractTags(List<String> descriptions) {
        // 키워드 맵 정의 (기존 로직 + 보강된 키워드)
        Map<String, List<String>> keywordMap = new HashMap<>();

        keywordMap.put("교사", List.of("교육", "교직"));
        keywordMap.put("교수", List.of("교육", "학문"));
        keywordMap.put("의사", List.of("의료", "건강"));
        keywordMap.put("간호", List.of("의료", "간호", "돌봄"));
        keywordMap.put("논리", List.of("논리", "분석", "수학"));
        keywordMap.put("분석", List.of("분석", "데이터", "논리"));
        keywordMap.put("소프트웨어", List.of("IT", "코딩", "컴퓨터"));
        keywordMap.put("하드웨어", List.of("기계", "전자", "컴퓨터"));
        keywordMap.put("프로그래밍", List.of("코딩", "IT", "개발"));
        keywordMap.put("컴퓨터", List.of("IT", "컴퓨터", "기술"));
        keywordMap.put("프로그램", List.of("IT", "코딩", "프로그래밍"));
        keywordMap.put("코딩", List.of("IT", "코딩", "프로그래밍"));
        keywordMap.put("AI", List.of("AI", "인공지능", "기술"));
        keywordMap.put("인공지능", List.of("AI", "인공지능", "기술"));
        keywordMap.put("데이터", List.of("데이터", "분석", "IT"));
        keywordMap.put("디자인", List.of("디자인", "미술", "창작"));
        keywordMap.put("예술", List.of("예술", "창작", "표현"));
        keywordMap.put("경영", List.of("경영", "비즈니스", "관리"));
        keywordMap.put("금융", List.of("금융", "경제", "투자"));
        keywordMap.put("법", List.of("법", "법률", "정의"));
        keywordMap.put("건축", List.of("건축", "설계", "공간"));
        keywordMap.put("체육", List.of("체육", "운동", "스포츠"));
        keywordMap.put("음악", List.of("음악", "예술", "공연"));
        keywordMap.put("언어", List.of("언어", "외국어", "소통"));
        keywordMap.put("영어", List.of("영어", "외국어", "언어"));
        keywordMap.put("일본", List.of("일본", "일본어", "외국어"));
        keywordMap.put("중국", List.of("중국", "중국어", "외국어"));
        keywordMap.put("역사", List.of("역사", "인문", "문화"));
        keywordMap.put("문화", List.of("문화", "인문", "예술"));
        keywordMap.put("과학", List.of("과학", "연구", "실험"));
        keywordMap.put("공학", List.of("공학", "기술", "엔지니어링"));
        keywordMap.put("게임", List.of("게임", "콘텐츠", "개발"));
        keywordMap.put("영화", List.of("영화", "미디어", "콘텐츠"));
        keywordMap.put("방송", List.of("방송", "미디어", "콘텐츠"));
        keywordMap.put("관광", List.of("관광", "여행", "서비스"));
        keywordMap.put("호텔", List.of("호텔", "서비스", "관광"));
        keywordMap.put("조리", List.of("조리", "요리", "식품"));
        keywordMap.put("패션", List.of("패션", "디자인", "의류"));
        keywordMap.put("웹툰", List.of("웹툰", "만화", "창작"));
        keywordMap.put("심리", List.of("심리", "상담", "치료"));

        Set<String> tags = new HashSet<>();
        // 리스트 설명글을 하나의 문자열로 합쳐서 검사
        String combinedText = String.join(" ", descriptions).toLowerCase();

        keywordMap.forEach((key, values) -> {
            if (combinedText.contains(key)) {
                tags.addAll(values);
            }
        });

        return new ArrayList<>(tags);
    }
}