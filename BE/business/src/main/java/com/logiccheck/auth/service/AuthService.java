package com.logiccheck.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.auth.dto.AuthResponse;
import com.logiccheck.auth.entity.Session;
import com.logiccheck.auth.repository.SessionRepository;
import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.global.security.JwtTokenProvider;
import com.logiccheck.user.dto.LoginRequest;
import com.logiccheck.user.dto.SignUpRequest;
import com.logiccheck.user.dto.UserResponse;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.entity.UserStatus;
import com.logiccheck.user.repository.UserRepository;

@Service
public class AuthService {
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        validatePassword(request.password());
        if (userRepository.existsByEmail(request.email())) {
            throw emailAlreadyExists();
        }

        User user;
        try {
            user = userRepository.saveAndFlush(User.create(request.email(), passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyExists();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String passwordHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !matches || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return UserResponse.from(user);
    }

    private AuthResponse issueTokens(User user) {
        String refreshToken = generateRefreshToken();
        sessionRepository.save(Session.create(
                user,
                hash(refreshToken),
                Instant.now().plus(REFRESH_TOKEN_TTL)));
        return AuthResponse.of(jwtTokenProvider.createAccessToken(user.getId()), refreshToken, user);
    }

    private void validatePassword(String password) {
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (password.length() < 8 || !hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, Map.of("field", "password"));
        }
    }

    private BusinessException emailAlreadyExists() {
        return new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, Map.of("field", "email"));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
