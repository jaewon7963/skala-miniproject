package com.logiccheck.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원가입 화면이 보내는 값. 이메일이 곧 아이디이고 이름은 받지 않는다. */
public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password,
        Boolean agreeTerms,
        Boolean agreePrivacy) {

    public boolean agreedToAll() {
        return Boolean.TRUE.equals(agreeTerms) && Boolean.TRUE.equals(agreePrivacy);
    }
}
