package org.example.catp.repository;

import org.example.catp.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // 학과 이름으로 찾기 (데이터 중복 방지용)
    Optional<Department> findByName(String name);
}