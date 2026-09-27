package com.smartattend.attendance;

public interface AttendanceProvider {
    AttendanceResult capture(AttendanceRequest request);
}