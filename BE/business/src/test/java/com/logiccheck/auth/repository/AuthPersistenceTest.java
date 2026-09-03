package com.logiccheck.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.business.BusinessApplication;
import com.logiccheck.auth.entity.Session;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.repository.UserRepository;

@DataJpaTest
@Testcontainers
@ContextConfiguration(classes = BusinessApplication.class)
class AuthPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Test
    void storesAndRotatesSession() {
        User user = userRepository.save(User.create("member@example.com", "bcrypt-hash"));
        Instant expiresAt = Instant.now().plus(Duration.ofDays(14));
        Session session = sessionRepository.save(Session.create(user, "old-hash", expiresAt));

        session.rotate("new-hash", expiresAt.plusSeconds(1));

        assertThat(sessionRepository.findByRefreshTokenHash("old-hash")).isEmpty();
        assertThat(sessionRepository.findByRefreshTokenHash("new-hash")).contains(session);
        assertThat(session.isActiveAt(Instant.now())).isTrue();
    }

    @Test
    void rejectsExpiredAndRevokedSession() {
        User user = userRepository.save(User.create("expired@example.com", "bcrypt-hash"));
        Session expired = Session.create(user, "expired-hash", Instant.now().minusSeconds(1));
        Session revoked = Session.create(user, "revoked-hash", Instant.now().plusSeconds(60));
        revoked.revoke(Instant.now());

        assertThat(expired.isActiveAt(Instant.now())).isFalse();
        assertThat(revoked.isActiveAt(Instant.now())).isFalse();
    }
}
