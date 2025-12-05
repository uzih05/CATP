package org.example.catp.service.calculator;

import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.AptitudeType;
import org.example.catp.entity.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StandardScoreCalculator implements ScoreCalculator {

    @Override
    public List<Double> calculate(List<Question> questions, List<Integer> answers) {
        double[] aptitudeSums = new double[AptitudeType.values().length];
        int[] aptitudeCounts = new int[AptitudeType.values().length];

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int answer = answers.get(i);

            // 1. 역채점 처리 (1<->5, 2<->4 ...)
            int score = q.isReverse() ? (6 - answer) : answer;

            // 2. 적성 인덱스 찾기 (Enum 사용)
            try {
                AptitudeType type = AptitudeType.fromDisplayName(q.getAptitudeType());
                aptitudeSums[type.getIndex()] += score;
                aptitudeCounts[type.getIndex()]++;
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 적성 타입 발견: {}", q.getAptitudeType());
            }
        }

        // 3. 평균 점수 계산 (소수점 첫째 자리 반올림)
        List<Double> finalScores = new ArrayList<>();
        for (int i = 0; i < AptitudeType.values().length; i++) {
            double avg = (aptitudeCounts[i] == 0) ? 0 : (aptitudeSums[i] / aptitudeCounts[i]);
            finalScores.add(Math.round(avg * 10) / 10.0);
        }

        return finalScores;
    }
}
