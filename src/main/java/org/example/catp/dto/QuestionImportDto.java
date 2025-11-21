package org.example.catp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.catp.entity.Question;

import java.util.List;

@Data
@NoArgsConstructor
public class QuestionImportDto {

    @JsonProperty("question_text")
    private String questionText;

    @JsonProperty("aptitude_type")
    private String aptitudeType;

    @JsonProperty("is_reverse")
    private boolean isReverse;

    @JsonProperty("question_order")
    private Integer questionOrder;

    @JsonProperty("tags")
    private List<String> tags; // JSON에서는 배열로 받음

    public Question toEntity(ObjectMapper objectMapper) {
        Question question = new Question();
        question.setQuestionText(this.questionText);
        question.setAptitudeType(this.aptitudeType);
        question.setReverse(this.isReverse);
        question.setQuestionOrder(this.questionOrder);

        try {
            // List<String> -> JSON String 변환 (예: "[\"독서\", \"글쓰기\"]")
            question.setTags(objectMapper.writeValueAsString(this.tags));
        } catch (JsonProcessingException e) {
            // 태그 변환 실패 시 빈 배열 문자열 저장
            question.setTags("[]");
        }

        return question;
    }
}