package com.logiccheck.user.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.logiccheck.user.entity.User;

public record UserResponse(String id, String email, String status, OffsetDateTime createdAt) {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getStatus().name(),
                user.getCreatedAt().atOffset(KST));
    }
}
