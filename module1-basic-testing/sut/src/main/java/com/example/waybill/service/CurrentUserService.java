package com.example.waybill.service;

import com.example.waybill.entity.User;
import com.example.waybill.repository.UserRepository;
import com.example.waybill.security.JwtUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public JwtUser jwtUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof JwtUser user) {
            return user;
        }
        throw new IllegalArgumentException("未登录");
    }

    public User user() {
        return userRepository.findById(jwtUser().userId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
