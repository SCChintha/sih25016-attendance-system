package com.smartattend.repository;

import com.smartattend.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByClientUuid(String clientUuid);
    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);
}