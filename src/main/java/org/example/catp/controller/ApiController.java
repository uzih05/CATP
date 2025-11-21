package org.example.catp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Question;
import org.example.catp.entity.TestResult;
import org.example.catp.repository.QuestionRepository;
import org.example.catp.repository.TestResultRepository;
import org.example.catp.service.CareerService;
import org.example.catp.util.IdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final QuestionRepository questionRepository;
    private final TestResultRepository testResultRepository;
    private final CareerService careerService;
    private final ObjectMapper objectMapper;

    // 1. 질문 목록 조회 API
    @GetMapping("/questions")
    public Map<String, Object> getQuestions() {
        List<Question> questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        return Map.of("questions", questions, "total", questions.size());
    }

    // 2. 검사 결과 제출 및 분석 API
    @PostMapping("/results")
    public ResponseEntity<Map<String, Object>> submitTest(@RequestBody Map<String, List<Integer>> payload) {
        try {
            List<Integer> answers = payload.get("answers");
            if (answers == null || answers.size() != 20) {
                return ResponseEntity.badRequest().body(Map.of("error", "답변은 20개여야 합니다."));
            }

            // 서비스 로직 실행 (점수 계산 및 학과 매칭)
            Map<String, Object> analysisResult = careerService.analyzeTest(answers);

            // 결과 저장 (Entity 생성)
            TestResult testResult = new TestResult();
            String resultId = IdGenerator.generate();

            // ID 중복 체크
            while(testResultRepository.existsById(resultId)) {
                resultId = IdGenerator.generate();
            }

            testResult.setId(resultId);
            testResult.setPersonalityType((String) analysisResult.get("personality"));

            // JSON 변환 후 저장
            testResult.setUserAnswers(objectMapper.writeValueAsString(answers));
            testResult.setUserScores(objectMapper.writeValueAsString(analysisResult.get("scores")));
            testResult.setInterestTags(objectMapper.writeValueAsString(analysisResult.get("interest_tags")));
            testResult.setTopDepartments(objectMapper.writeValueAsString(analysisResult.get("top_departments")));
            testResult.setWorstDepartments(objectMapper.writeValueAsString(analysisResult.get("worst_departments")));
            testResult.setSimilarDepartments(objectMapper.writeValueAsString(analysisResult.get("similar_departments")));

            testResultRepository.save(testResult);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>(analysisResult);
            response.put("id", resultId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결과 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. 결과 조회 API (공유 링크용)
    // [수정됨] 반환 타입을 ResponseEntity<?>로 변경하여 404 응답(Void)과 성공 응답(Map)을 모두 수용
    @GetMapping("/results/{id}")
    public ResponseEntity<?> getResult(@PathVariable String id) {
        return testResultRepository.findById(id)
                .map(result -> {
                    try {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", result.getId());
                        response.put("personality", result.getPersonalityType());
                        response.put("created_at", result.getCreatedAt());

                        response.put("scores", objectMapper.readValue(result.getUserScores(), new TypeReference<List<Double>>(){}));
                        response.put("interest_tags", objectMapper.readValue(result.getInterestTags(), new TypeReference<List<String>>(){}));
                        response.put("top_departments", objectMapper.readValue(result.getTopDepartments(), new TypeReference<List<Map<String, Object>>>(){}));
                        response.put("worst_departments", objectMapper.readValue(result.getWorstDepartments(), new TypeReference<List<Map<String, Object>>>(){}));

                        if (result.getSimilarDepartments() != null) {
                            response.put("similar_departments", objectMapper.readValue(result.getSimilarDepartments(), new TypeReference<List<Map<String, Object>>>(){}));
                        } else {
                            response.put("similar_departments", List.of());
                        }

                        Map<String, String> summary = new HashMap<>();
                        summary.put("personality", result.getPersonalityType() + " 학생입니다.");
                        response.put("summary", summary);

                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        log.error("데이터 파싱 오류", e);
                        // 에러 발생 시 에러 메시지 맵 반환
                        return ResponseEntity.internalServerError().body((Object) Map.of("error", "데이터 처리 중 오류가 발생했습니다."));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}