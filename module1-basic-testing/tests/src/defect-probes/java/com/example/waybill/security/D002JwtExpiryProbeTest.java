package com.example.waybill.security;

import com.example.waybill.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class D002JwtExpiryProbeTest {
    @Test
    void tokenAtItsExpirationSecondMustBeRejected() {
        JwtService service = new JwtService("test-secret", 0);
        for (int attempt = 0; attempt < 100; attempt++) {
            long issuedSecond = Instant.now().getEpochSecond();
            String token = service.issue(7L, "alice", Role.ADMIN);
            boolean accepted = service.parse(token).isPresent();
            if (Instant.now().getEpochSecond() == issuedSecond) {
                System.out.println("D-002 issuedSecond=" + issuedSecond
                        + " actualTokenAccepted=" + accepted);
                assertThat(accepted)
                        .as("D-002 token must be rejected at its expiration second")
                        .isFalse();
                return;
            }
        }
        fail("D-002 could not complete issue and parse within one second; rerun the probe");
    }
}
