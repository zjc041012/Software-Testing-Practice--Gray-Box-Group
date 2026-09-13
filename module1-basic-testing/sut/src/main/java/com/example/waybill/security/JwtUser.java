package com.example.waybill.security;

import com.example.waybill.entity.Role;

public record JwtUser(Long userId, String username, Role role) {
}
