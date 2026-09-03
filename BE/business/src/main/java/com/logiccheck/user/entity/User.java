package com.logiccheck.user.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
    }

    private User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public static User create(String email, String passwordHash) {
        return new User(email, passwordHash);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 탈퇴 표시. 데이터를 즉시 지우지 않고 상태만 바꾼다 — 복구 요청과 감사 기록을 위해
     * 실제 삭제는 유예 기간이 지난 뒤 별도로 처리한다.
     */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
