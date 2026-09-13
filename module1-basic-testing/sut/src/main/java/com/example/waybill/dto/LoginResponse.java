package com.example.waybill.dto;

import com.example.waybill.entity.Role;

public record LoginResponse(String token, Long userId, String username, String realName, Role role, Long orgId) {
}
