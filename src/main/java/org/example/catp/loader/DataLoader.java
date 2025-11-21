package org.example.catp.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.dto.DepartmentImportDto;
import org.example.catp.dto.QuestionImportDto; // 추가됨
import org.example.catp.entity.Department;
import org.example.catp.entity.Question;
import org.example.catp.repository.DepartmentRepository;
import org.example.catp.repository.QuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

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
        log.info("📝 질문 데이터 로딩 중... (questions.json)");
        try {
            InputStream inputStream = getClass().getResourceAsStream("/questions.json");

            // [수정] DTO를 통해 배열 데이터를 안전하게 받음
            List<QuestionImportDto> dtos = objectMapper.readValue(inputStream, new TypeReference<>() {});

            // DTO -> Entity 변환
            List<Question> questions = dtos.stream()
                    .map(dto -> dto.toEntity(objectMapper))
                    .collect(Collectors.toList());

            questionRepository.saveAll(questions);
            log.info("✅ 질문 {}개 로딩 완료!", questions.size());
        } catch (Exception e) {
            log.error("❌ 질문 데이터 로딩 실패: {}", e.getMessage(), e);
        }
    }

    private void initDepartments() {
        log.info("🏫 학과 데이터 로딩 중... (jj_departments_with_scores.json)");
        try {
            InputStream inputStream = getClass().getResourceAsStream("/jj_departments_with_scores.json");

            List<DepartmentImportDto> dtos = objectMapper.readValue(inputStream, new TypeReference<>() {});

            List<Department> departments = dtos.stream()
                    .map(dto -> dto.toEntity(objectMapper))
                    .collect(Collectors.toList());

            departmentRepository.saveAll(departments);
            log.info("✅ 학과 {}개 로딩 완료!", departments.size());

        } catch (Exception e) {
            log.error("❌ 학과 데이터 로딩 실패: {}", e.getMessage(), e);
        }
    }
}