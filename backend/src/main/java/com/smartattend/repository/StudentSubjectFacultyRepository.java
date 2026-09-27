package com.smartattend.repository;

import com.smartattend.domain.StudentSubjectFaculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface StudentSubjectFacultyRepository extends JpaRepository<StudentSubjectFaculty, Long> {
    List<StudentSubjectFaculty> findByStudentId(Long studentId);
    
    @Modifying
    void deleteByStudentId(Long studentId);
}
