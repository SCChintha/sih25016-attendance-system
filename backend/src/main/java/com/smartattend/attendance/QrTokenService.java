package com.smartattend.attendance;

import com.smartattend.domain.AttendanceSession;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class QrTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 20;

    public String create(AttendanceSession session) {
        long expiry = Instant.now().getEpochSecond() + TOKEN_LIFETIME_SECONDS;
        String body = session.getId() + ":" + expiry;
        return body + "." + sign(body, session.getQrSeed());
    }

    public boolean isValid(String token, AttendanceSession session) {
        String[] parts = token == null ? new String[0] : token.split("\\.", -1);
        if (parts.length != 2) return false;
        try {
            String[] body = parts[0].split(":", -1);
            long expiry = Long.parseLong(body[1]);
            return body.length == 2 && body[0].equals(session.getId().toString())
                && expiry >= Instant.now().getEpochSecond() && sign(parts[0], session.getQrSeed()).equals(parts[1]);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign QR token", exception);
        }
    }
}