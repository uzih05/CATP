package org.example.catp.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Department;
import org.example.catp.entity.Question;
import org.example.catp.repository.DepartmentRepository;
import org.example.catp.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        // 1. 질문 데이터 초기화
        if (questionRepository.count() == 0) {
            initQuestions();
        }

        // 2. 학과 데이터 초기화
        if (departmentRepository.count() == 0) {
            initDepartments();
        }
    }

    private void initQuestions() {
        log.info("📝 질문 데이터 로딩 중...");
        List<Question> questions = new ArrayList<>();

        // 1. 언어능력
        addQ(questions, 1, "책을 읽거나 글을 쓰는 것을 좋아한다.", "언어능력", false, List.of("독서", "글쓰기", "문학"));
        addQ(questions, 2, "다른 사람에게 내 생각을 말이나 글로 표현하는 것이 어렵다.", "언어능력", true, List.of());

        // 2. 논리/분석력
        addQ(questions, 3, "복잡한 문제를 단계별로 분석하고 해결하는 것을 좋아한다.", "논리/분석력", false, List.of("논리", "분석", "문제해결"));
        addQ(questions, 4, "숫자나 데이터를 다루는 일은 나와 맞지 않는다.", "논리/분석력", true, List.of());

        // 3. 창의력
        addQ(questions, 5, "새로운 아이디어나 독창적인 방법을 생각해내는 것을 즐긴다.", "창의력", false, List.of("창의", "아이디어", "기획"));
        addQ(questions, 6, "정해진 틀이나 규칙을 따르는 것이 더 편하다.", "창의력", true, List.of());

        // 4. 사회성/공감능력
        addQ(questions, 7, "다른 사람의 감정을 잘 이해하고 공감할 수 있다.", "사회성/공감능력", false, List.of("소통", "공감", "사회성"));
        addQ(questions, 8, "혼자 일하는 것이 다른 사람과 협력하는 것보다 편하다.", "사회성/공감능력", true, List.of());

        // 5. 주도성/리더십
        addQ(questions, 9, "팀 프로젝트에서 리더 역할을 맡는 것을 선호한다.", "주도성/리더십", false, List.of("리더십", "주도", "팀워크"));
        addQ(questions, 10, "다른 사람을 이끌거나 설득하는 것이 부담스럽다.", "주도성/리더십", true, List.of());

        // 6. 신체-활동성
        addQ(questions, 11, "운동이나 신체 활동을 하는 것을 좋아한다.", "신체-활동성", false, List.of("운동", "활동", "체육"));
        addQ(questions, 12, "오래 앉아서 일하는 것이 나에게 더 잘 맞는다.", "신체-활동성", true, List.of());

        // 7. 예술감각/공간지각
        addQ(questions, 13, "그림, 음악, 디자인 등 예술적인 활동에 관심이 많다.", "예술감각/공간지각", false, List.of("예술", "디자인", "미술"));
        addQ(questions, 14, "색상이나 형태의 조화를 생각하는 것이 어렵다.", "예술감각/공간지각", true, List.of());

        // 8. 체계성/꼼꼼함
        addQ(questions, 15, "일을 계획적이고 체계적으로 처리하는 것을 선호한다.", "체계성/꼼꼼함", false, List.of("체계", "계획", "꼼꼼"));
        addQ(questions, 16, "세부적인 것보다 큰 그림을 보는 것이 더 중요하다고 생각한다.", "체계성/꼼꼼함", true, List.of());

        // 9. 탐구심
        addQ(questions, 17, "새로운 지식을 배우고 연구하는 것을 좋아한다.", "탐구심", false, List.of("연구", "학습", "탐구"));
        addQ(questions, 18, "'왜 그럴까?'라는 의문을 가지고 깊이 파고드는 것이 번거롭게 느껴진다.", "탐구심", true, List.of());

        // 10. 문제해결능력
        addQ(questions, 19, "어려운 문제에 부딪혔을 때 포기하지 않고 해결 방법을 찾는다.", "문제해결능력", false, List.of("문제해결", "끈기", "도전"));
        addQ(questions, 20, "예상치 못한 상황이 생기면 당황하고 어떻게 대처해야 할지 모르겠다.", "문제해결능력", true, List.of());

        questionRepository.saveAll(questions);
        log.info("✅ 질문 {}개 로딩 완료!", questions.size());
    }

    private void addQ(List<Question> list, int order, String text, String type, boolean rev, List<String> tags) {
        Question q = new Question();
        q.setQuestionOrder(order);
        q.setQuestionText(text);
        q.setAptitudeType(type);
        q.setReverse(rev);
        try {
            q.setTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            q.setTags("[]");
        }
        list.add(q);
    }

    private void initDepartments() {
        log.info("🏫 학과 데이터 로딩 중...");
        try {
            InputStream inputStream = getClass().getResourceAsStream("/jj_departments_with_scores.json");
            List<Map<String, Object>> rawData = objectMapper.readValue(inputStream, new TypeReference<>() {});

            List<Department> departments = new ArrayList<>();

            for (Map<String, Object> data : rawData) {
                Department dept = new Department();
                String name = (String) data.get("학과");
                List<String> aptitudeDesc = (List<String>) data.get("적성");
                List<Integer> scores = (List<Integer>) data.get("적성점수");

                dept.setName(name);
                dept.setUrl((String) data.get("URL"));
                dept.setAptitudeScores(objectMapper.writeValueAsString(scores));
                dept.setDescription(objectMapper.writeValueAsString(aptitudeDesc));

                // Python 로직 이식: 태그 추출 및 카테고리 추론
                List<String> tags = extractTags(aptitudeDesc);
                String category = inferCategory(name);

                dept.setTags(objectMapper.writeValueAsString(tags));
                dept.setCategory(category);

                departments.add(dept);
            }

            departmentRepository.saveAll(departments);
            log.info("✅ 학과 {}개 로딩 완료!", departments.size());

        } catch (Exception e) {
            log.error("❌ 학과 데이터 로딩 실패: {}", e.getMessage());
        }
    }

    // Python의 extract_department_tags 함수 이식
    private List<String> extractTags(List<String> descriptions) {
        Map<String, List<String>> keywordMap = new HashMap<>();
        keywordMap.put("교사", List.of("교육", "교직"));
        keywordMap.put("교수", List.of("교육", "학문"));
        keywordMap.put("의사", List.of("의료", "건강"));
        keywordMap.put("간호", List.of("의료", "간호", "돌봄"));
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
        String combinedText = String.join(" ", descriptions).toLowerCase();

        keywordMap.forEach((key, values) -> {
            if (combinedText.contains(key)) {
                tags.addAll(values);
            }
        });

        return new ArrayList<>(tags);
    }

    // Python의 infer_category 함수 이식
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
}