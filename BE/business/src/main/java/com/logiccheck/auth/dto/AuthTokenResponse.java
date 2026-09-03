package com.logiccheck.auth.dto;

import com.logiccheck.user.entity.User;

/**
 * 로그인·회원가입 응답.
 *
 * <p>키 이름이 {@code token} 인 것은 화면이 그 이름으로 값을 꺼내기 때문이다.
 * 리프레시 토큰은 화면에 개념 자체가 없어 내려보내지 않는다.
 */
public record AuthTokenResponse(String token, MeResponse user) {

    public static AuthTokenResponse of(String token, User user) {
        return new AuthTokenResponse(token, MeResponse.from(user));
    }
}
