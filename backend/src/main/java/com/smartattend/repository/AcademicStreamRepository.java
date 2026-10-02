package com.smartattend.repository;

import com.smartattend.domain.AcademicStream;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AcademicStreamRepository extends JpaRepository<AcademicStream, Long> {
    List<AcademicStream> findByGradeLevelIdAndActiveTrueOrderByNameAsc(Long gradeId);
    List<AcademicStream> findByActiveTrueOrderByGradeLevelDisplayOrderAscNameAsc();
    boolean existsByGradeLevelIdAndCodeIgnoreCase(Long gradeId, String code);
}
