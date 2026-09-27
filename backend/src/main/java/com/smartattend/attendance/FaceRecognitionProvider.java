package com.smartattend.attendance;

import org.springframework.stereotype.Component;

@Component
public class FaceRecognitionProvider implements AttendanceProvider {
    @Override
    public AttendanceResult capture(AttendanceRequest request) {
        throw new UnsupportedOperationException("Face verification requires the external face service integration");
    }
}