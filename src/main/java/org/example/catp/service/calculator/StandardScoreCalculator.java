package org.example.catp.service.calculator;

import lombok.extern.slf4j.Slf4j;
import org.example.catp.entity.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StandardScoreCalculator implements ScoreCalculator {

    // 적성 타입 순서 (DB 및 로직 공통 기준)
    public static final String[] APTITUDE_TYPES = {
            "언어능력", "논리/분석력", "창의력", "사회성/공감능력", "주도성/리더십",
            "신체-활동성", "예술감각/공간지각", "체계성/꼼꼼함", "탐구심", "문제해결능력"
    };

    @Override
    public List<Double> calculate(List<Question> questions, List<Integer> answers) {
        double[] aptitudeSums = new double[10];
        int[] aptitudeCounts = new int[10];

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int answer = answers.get(i);

            // 1. 역채점 처리 (1<->5, 2<->4 ...)
            // 질문이 부정형(isReverse=true)인 경우 점수를 뒤집습니다.
            int score = q.isReverse() ? (6 - answer) : answer;

            // 2. 적성 인덱스 찾기
            int typeIndex = getAptitudeIndex(q.getAptitudeType());
            if (typeIndex != -1) {
                aptitudeSums[typeIndex] += score;
                aptitudeCounts[typeIndex]++;
            } else {
                log.warn("알 수 없는 적성 타입 발견: {}", q.getAptitudeType());
            }
        }

        // 3. 평균 점수 계산 (소수점 첫째 자리 반올림)
        List<Double> finalScores = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            double avg = (aptitudeCounts[i] == 0) ? 0 : (aptitudeSums[i] / aptitudeCounts[i]);
            finalScores.add(Math.round(avg * 10) / 10.0);
        }

        return finalScores;
    }

    private int getAptitudeIndex(String type) {
        for (int i = 0; i < APTITUDE_TYPES.length; i++) {
            if (APTITUDE_TYPES[i].equals(type)) return i;
        }
        return -1;
    }
}