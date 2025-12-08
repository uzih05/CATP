package org.example.catp.repository;

import org.example.catp.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, String> {

    // "입력한 시간(cutoffDate)보다 이전에(Before) 생성된(CreatedAt) 데이터 삭제(delete)"
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}