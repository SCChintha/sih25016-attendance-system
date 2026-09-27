package com.smartattend.attendance;

import org.springframework.stereotype.Component;

@Component
public class FingerprintProvider implements AttendanceProvider {
    @Override
    public AttendanceResult capture(AttendanceRequest request) {
        throw new UnsupportedOperationException("Fingerprint hardware integration is not configured");
    }
}