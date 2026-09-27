package com.smartattend.repository;

import com.smartattend.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentId(Long departmentId);
}