package com.logiccheck.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.logiccheck.auth.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    Optional<Session> findByRefreshTokenHashAndUserId(String refreshTokenHash, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update Session s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
