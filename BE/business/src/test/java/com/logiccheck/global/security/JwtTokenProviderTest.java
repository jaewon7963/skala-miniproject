package com.logiccheck.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtTokenProviderTest {
    private final JwtTokenProvider tokenProvider =
            new JwtTokenProvider("test-secret-key-with-at-least-32-bytes");

    @Test
    void accessTokenContainsUserAndExpiresAfterOneDay() {
        Jwt jwt = tokenProvider.decoder().decode(tokenProvider.createAccessToken(17L));

        assertThat(jwt.getSubject()).isEqualTo("17");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
    }
}
