package com.logiccheck.auth.dto;

public record RefreshResponse(String accessToken, String refreshToken) {
}
