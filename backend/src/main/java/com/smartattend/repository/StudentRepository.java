package com.smartattend.repository;

import com.smartattend.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    long countBySection_Id(Long sectionId);
    java.util.List<Student> findBySection_Id(Long sectionId);
}
