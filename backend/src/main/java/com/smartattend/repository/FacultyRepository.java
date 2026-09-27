package com.smartattend.repository;

import com.smartattend.domain.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
	java.util.Optional<Faculty> findByUserId(Long userId);
}