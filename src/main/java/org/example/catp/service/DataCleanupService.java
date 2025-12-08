package org.example.catp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catp.repository.TestResultRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final TestResultRepository testResultRepository;

    // 매일 새벽 4시에 자동으로 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void deleteOldResults() {
        // 기준 날짜를 '30일 전'으로 설정
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // 팁: 정확히 '지난 달' 같은 날짜로 하려면 .minusMonths(1)을 써도 됩니다.
        // LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(1);

        log.info("🧹 데이터 정리 시작: {} 이전에 생성된(30일 지난) 데이터를 삭제합니다.", cutoffDate);

        try {
            testResultRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("30일이 지난 오래된 데이터 삭제 완료.");
        } catch (Exception e) {
            log.error("데이터 삭제 중 오류 발생", e);
        }
    }
}