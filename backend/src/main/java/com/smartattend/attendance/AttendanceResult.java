package com.smartattend.attendance;

import com.smartattend.domain.AttendanceMethod;
import java.time.Instant;

public record AttendanceResult(Long studentId, double confidence, Instant timestamp, AttendanceMethod method) { }