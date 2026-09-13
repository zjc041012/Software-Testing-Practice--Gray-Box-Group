package com.example.waybill.service;

import com.example.waybill.dto.LoginRequest;
import com.example.waybill.dto.LoginResponse;
import com.example.waybill.entity.Organization;
import com.example.waybill.entity.Role;
import com.example.waybill.entity.User;
import com.example.waybill.repository.UserRepository;
import com.example.waybill.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        Organization organization = new Organization();
        organization.setId(18L);
        user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        user.setRealName("Alice");
        user.setRole(Role.CONSIGNOR);
        user.setOrganization(organization);
    }

    @Test
    void correctCredentialsReturnUserInfoAndJwt() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.issue(7L, "alice", Role.CONSIGNOR)).thenReturn("token-1");

        LoginResponse response = authService.login(new LoginRequest("alice", "correct-password"));

        assertThat(response.token()).isEqualTo("token-1");
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.realName()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo(Role.CONSIGNOR);
        assertThat(response.orgId()).isEqualTo(18L);
    }

    @Test
    void anUnknownUsernameIsRejected() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名或密码错误");
        verifyNoInteractions(jwtService);
    }

    @Test
    void aWrongPasswordIsRejected() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名或密码错误");
        verifyNoInteractions(jwtService);
    }
}
