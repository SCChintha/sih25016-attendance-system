package com.smartattend.repository;

import com.smartattend.domain.AttendanceSession;
import com.smartattend.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByFaculty_IdAndStatus(Long facultyId, SessionStatus status);
    List<AttendanceSession> findByFaculty_Id(Long facultyId);
    List<AttendanceSession> findBySection_Id(Long sectionId);
}
