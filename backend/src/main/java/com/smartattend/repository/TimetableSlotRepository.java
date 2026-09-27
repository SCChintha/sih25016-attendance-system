package com.smartattend.repository;

import com.smartattend.domain.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {
    java.util.List<TimetableSlot> findByFaculty_Id(Long facultyId);
    java.util.List<TimetableSlot> findBySection_Id(Long sectionId);
}
