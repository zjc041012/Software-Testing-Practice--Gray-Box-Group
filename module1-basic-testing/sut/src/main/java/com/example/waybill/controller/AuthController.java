package com.example.waybill.controller;

import com.example.waybill.dto.ApiResponse;
import com.example.waybill.dto.LoginRequest;
import com.example.waybill.dto.LoginResponse;
import com.example.waybill.service.AuthService;
import com.example.waybill.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(currentUserService.user());
    }
}
