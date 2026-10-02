package com.smartattend.repository;

import com.smartattend.domain.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<GradeLevel> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
