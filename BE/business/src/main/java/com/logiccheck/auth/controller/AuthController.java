package com.logiccheck.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.logiccheck.auth.dto.AuthTokenResponse;
import com.logiccheck.auth.dto.MeResponse;
import com.logiccheck.auth.dto.OkResponse;
import com.logiccheck.auth.dto.PasswordChangeRequest;
import com.logiccheck.auth.dto.SignupRequest;
import com.logiccheck.auth.dto.WithdrawRequest;
import com.logiccheck.auth.service.AuthService;
import com.logiccheck.global.security.CurrentUser;
import com.logiccheck.user.dto.LoginRequest;

import jakarta.validation.Valid;

/**
 * 화면이 쓰는 인증 경로.
 *
 * <p>로그인 요청에는 화면이 "로그인 상태 유지" 체크값을 같이 보내는데 서버는 쓰지 않는다.
 * 모르는 필드는 무시되므로 그대로 받아 넘긴다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int PURGE_AFTER_DAYS = 30;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.signin(request);
    }

    /** 액세스 토큰은 상태를 갖지 않으므로 서버가 할 일은 세션 정리뿐이다. */
    @PostMapping("/logout")
    public OkResponse logout(@CurrentUser Long userId) {
        authService.logout(userId, null);
        return OkResponse.success();
    }

    @GetMapping("/me")
    public MeResponse me(@CurrentUser Long userId) {
        return authService.profile(userId);
    }

    @PatchMapping("/me/password")
    public OkResponse changePassword(@CurrentUser Long userId, @RequestBody PasswordChangeRequest request) {
        authService.changePassword(userId, request);
        return OkResponse.success();
    }

    /** 화면이 DELETE 에도 본문을 실어 비밀번호를 보낸다. */
    @DeleteMapping("/me")
    public OkResponse withdraw(@CurrentUser Long userId,
                                @RequestBody(required = false) WithdrawRequest request) {
        authService.withdraw(userId, request);
        return OkResponse.withdrawn(PURGE_AFTER_DAYS);
    }
}
