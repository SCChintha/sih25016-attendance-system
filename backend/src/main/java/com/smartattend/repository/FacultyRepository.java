package com.smartattend.repository;

import com.smartattend.domain.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
	java.util.Optional<Faculty> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Faculty f where f.user.id = :userId")
    java.util.Optional<Faculty> findByUserIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Faculty f where f.id = :facultyId")
    java.util.Optional<Faculty> findByIdForUpdate(@Param("facultyId") Long facultyId);
}
