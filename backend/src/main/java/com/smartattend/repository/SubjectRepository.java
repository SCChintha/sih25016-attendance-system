package com.smartattend.repository;

import com.smartattend.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDepartmentId(Long departmentId);
    List<Subject> findByActiveTrueAndStreamIsNotNullOrderByNameAsc();
    List<Subject> findByStreamIdOrderByNameAsc(Long streamId);
    List<Subject> findByActiveTrueAndStream_IdOrderByNameAsc(Long streamId);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subject s join fetch s.stream stream join fetch stream.gradeLevel where s.id in :ids")
    List<Subject> findAllForUpdateByIdIn(@Param("ids") List<Long> ids);
}
