package org.example.catp.repository;

import org.example.catp.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, String> {
    // 기본 CRUD 기능(저장, 조회, 삭제)은 JpaRepository가 자동으로 제공하므로
    // 추가 코드가 필요 없습니다.
}