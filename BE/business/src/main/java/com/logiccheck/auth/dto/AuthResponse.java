package com.logiccheck.auth.dto;

import com.logiccheck.user.dto.UserResponse;
import com.logiccheck.user.entity.User;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
    public static AuthResponse of(String accessToken, String refreshToken, User user) {
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
