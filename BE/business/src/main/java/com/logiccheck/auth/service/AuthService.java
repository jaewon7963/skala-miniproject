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
import com.logiccheck.auth.dto.AuthTokenResponse;
import com.logiccheck.auth.dto.LogoutRequest;
import com.logiccheck.auth.dto.MeResponse;
import com.logiccheck.auth.dto.PasswordChangeRequest;
import com.logiccheck.auth.dto.SignupRequest;
import com.logiccheck.auth.dto.WithdrawRequest;
import com.logiccheck.auth.dto.RefreshRequest;
import com.logiccheck.auth.dto.RefreshResponse;
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
        return issueTokens(authenticate(request));
    }

    /**
     * 존재하지 않는 이메일이든 비밀번호가 틀렸든 같은 시간이 걸리도록 항상 해시를 비교한다.
     * 응답 시간 차이로 가입 여부를 알아내지 못하게 하기 위함이다.
     */
    private User authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String passwordHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !matches || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return user;
    }

    /* ------------------------------------------------------------------ */
    /* 화면이 호출하는 /api/auth/* 경로                                      */
    /* ------------------------------------------------------------------ */

    @Transactional
    public AuthTokenResponse signup(SignupRequest request) {
        if (!request.agreedToAll()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "agreeTerms"));
        }
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
        return AuthTokenResponse.of(issueAccessToken(user), user);
    }

    @Transactional
    public AuthTokenResponse signin(LoginRequest request) {
        User user = authenticate(request);
        return AuthTokenResponse.of(issueAccessToken(user), user);
    }

    @Transactional(readOnly = true)
    public MeResponse profile(Long userId) {
        return MeResponse.from(activeUserOrThrow(userId));
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = activeUserOrThrow(userId);
        verifyPassword(user, request == null ? null : request.currentPassword());
        validatePassword(request.newPassword());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // 세션 정리 쿼리가 영속성 컨텍스트를 비우기 때문에, 그 전에 변경을 반영해야
        // 방금 바꾼 비밀번호가 조용히 사라지지 않는다.
        userRepository.flush();
        sessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    /**
     * 탈퇴 요청. 즉시 삭제하지 않고 상태만 바꾸고 세션을 끊는다.
     * 실제 데이터 정리는 유예 기간이 지난 뒤 별도로 처리한다.
     */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = activeUserOrThrow(userId);
        verifyPassword(user, request == null ? null : request.password());
        user.withdraw();
        userRepository.flush();
        sessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    private User activeUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private void verifyPassword(User user, String rawPassword) {
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private String issueAccessToken(User user) {
        sessionRepository.save(Session.create(user, hash(generateRefreshToken()),
                Instant.now().plus(REFRESH_TOKEN_TTL)));
        return jwtTokenProvider.createAccessToken(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return UserResponse.from(user);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        Instant now = Instant.now();
        Session session = sessionRepository.findByRefreshTokenHash(hash(request.refreshToken()))
                .filter(found -> found.isActiveAt(now))
                .filter(found -> found.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        String refreshToken = generateRefreshToken();
        session.rotate(hash(refreshToken), now.plus(REFRESH_TOKEN_TTL));
        String accessToken = jwtTokenProvider.createAccessToken(session.getUser().getId());
        return new RefreshResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        Instant now = Instant.now();
        if (request == null || request.refreshToken() == null) {
            sessionRepository.revokeAllByUserId(userId, now);
            return;
        }
        if (request.refreshToken().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "refreshToken"));
        }
        sessionRepository.findByRefreshTokenHashAndUserId(hash(request.refreshToken()), userId)
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> session.revoke(now));
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
        if (password == null) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, Map.of("field", "password"));
        }
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
