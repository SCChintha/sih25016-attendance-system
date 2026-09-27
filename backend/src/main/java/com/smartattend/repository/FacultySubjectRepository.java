package com.smartattend.repository;

import com.smartattend.domain.FacultySubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface FacultySubjectRepository extends JpaRepository<FacultySubject, Long> {
    List<FacultySubject> findBySubjectId(Long subjectId);
    List<FacultySubject> findBySubjectIdIn(List<Long> subjectIds);
    List<FacultySubject> findByFacultyId(Long facultyId);
    boolean existsByFacultyIdAndSubjectId(Long facultyId, Long subjectId);
    
    @Modifying
    void deleteByFacultyId(Long facultyId);
}

