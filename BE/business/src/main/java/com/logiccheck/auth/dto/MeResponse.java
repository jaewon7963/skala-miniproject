package com.logiccheck.auth.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.logiccheck.user.entity.User;

/**
 * 화면이 다루는 사용자 표현.
 *
 * <p>{@code organization}은 저장하는 값이 아니라 이메일 도메인에서 뽑는다.
 * 소속을 따로 입력받지 않으면서도 헤더에 회사 표시를 하기 위한 절충이다.
 */
public record MeResponse(String id, String email, String organization, OffsetDateTime createdAt) {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static MeResponse from(User user) {
        return new MeResponse(
                String.valueOf(user.getId()),
                user.getEmail(),
                organizationOf(user.getEmail()),
                user.getCreatedAt().atOffset(KST));
    }

    private static String organizationOf(String email) {
        int at = email.indexOf('@');
        return at < 0 || at == email.length() - 1 ? null : email.substring(at + 1);
    }
}
