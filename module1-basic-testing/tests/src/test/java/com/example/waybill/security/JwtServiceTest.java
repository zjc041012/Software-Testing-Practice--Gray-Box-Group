package com.example.waybill.security;

import com.example.waybill.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService("test-secret", 10);

    @Test
    void issueAndParseReturnsTheOriginalUserClaims() {
        String token = jwtService.issue(7L, "alice", Role.ADMIN);

        Optional<JwtUser> parsed = jwtService.parse(token);

        assertThat(parsed).isPresent().contains(new JwtUser(7L, "alice", Role.ADMIN));
    }

    @Test
    void aModifiedSignatureIsRejected() {
        String token = jwtService.issue(7L, "alice", Role.ADMIN);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void anExpiredTokenIsRejected() {
        JwtService expiredService = new JwtService("test-secret", -1);

        String token = expiredService.issue(7L, "alice", Role.ADMIN);

        assertThat(expiredService.parse(token)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "a.b.c", "%%%.___", "a."})
    void malformedTokensAreRejected(String token) {
        assertThat(jwtService.parse(token)).isEmpty();
    }
}
