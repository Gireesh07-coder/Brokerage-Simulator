package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.Role;
import com.gireesh.brokerage.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        LocalDateTime createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
