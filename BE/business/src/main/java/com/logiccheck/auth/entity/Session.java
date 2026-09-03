package com.logiccheck.auth.entity;

import java.time.Instant;

import com.logiccheck.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Session() {
    }

    private Session(User user, String refreshTokenHash, Instant expiresAt) {
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    public static Session create(User user, String refreshTokenHash, Instant expiresAt) {
        return new Session(user, refreshTokenHash, expiresAt);
    }

    public void rotate(String refreshTokenHash, Instant expiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
