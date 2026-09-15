package com.example.waybill.security;

import com.example.waybill.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtService {
    private final byte[] secret;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt-secret}") String secret,
                      @Value("${app.jwt-expiration-minutes}") long expirationMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
    }

    public String issue(Long userId, String username, Role role) {
        long exp = Instant.now().plusSeconds(expirationMinutes * 60).getEpochSecond();
        String payload = userId + "|" + username + "|" + role.name() + "|" + exp;
        String encodedPayload = b64(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public Optional<JwtUser> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
                return Optional.empty();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split("\\|");
            if (fields.length != 4 || Long.parseLong(fields[3]) <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new JwtUser(Long.parseLong(fields[0]), fields[1], Role.valueOf(fields[2])));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return b64(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token signing failed", e);
        }
    }

    private String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
