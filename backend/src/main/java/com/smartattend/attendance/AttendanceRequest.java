package com.smartattend.attendance;

public record AttendanceRequest(Long sessionId, Long studentId, String proof) { }