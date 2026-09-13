package com.example.waybill.service;

import com.example.waybill.dto.LoginRequest;
import com.example.waybill.dto.LoginResponse;
import com.example.waybill.entity.User;
import com.example.waybill.repository.UserRepository;
import com.example.waybill.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtService.issue(user.getId(), user.getUsername(), user.getRole());
        Long orgId = user.getOrganization() == null ? null : user.getOrganization().getId();
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(), orgId);
    }
}
