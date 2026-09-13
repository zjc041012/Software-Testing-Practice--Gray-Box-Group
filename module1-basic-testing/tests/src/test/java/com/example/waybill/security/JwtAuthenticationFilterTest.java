package com.example.waybill.security;

import com.example.waybill.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = new JwtService("test-secret", 10);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenCreatesAuthenticationWithTheRoleAuthority() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.issue(7L, "alice", Role.ADMIN));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] continued = {false};
        FilterChain chain = (req, res) -> continued[0] = true;

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new JwtUser(7L, "alice", Role.ADMIN));
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(continued[0]).isTrue();
    }

    @Test
    void expiredBearerTokenDoesNotAuthenticateTheRequest() throws Exception {
        JwtService expiredJwtService = new JwtService("test-secret", -1);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(expiredJwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + expiredJwtService.issue(7L, "alice", Role.ADMIN));
        boolean[] continued = {false};

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> continued[0] = true);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(continued[0]).isTrue();
    }

    @Test
    void aNonBearerAuthorizationHeaderIsIgnored() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        boolean[] continued = {false};

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> continued[0] = true);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(continued[0]).isTrue();
    }

    @Test
    void aMalformedBearerTokenIsIgnoredAndTheChainStillRuns() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed-token");
        boolean[] continued = {false};

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> continued[0] = true);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(continued[0]).isTrue();
    }
}
