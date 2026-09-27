package com.smartattend.attendance;

import com.smartattend.domain.AttendanceMethod;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class QrCodeProvider implements AttendanceProvider {
    @Override
    public AttendanceResult capture(AttendanceRequest request) {
        return new AttendanceResult(request.studentId(), 1.0, Instant.now(), AttendanceMethod.QR);
    }
}