package com.logiccheck.global.common;

import com.logiccheck.global.exception.ErrorCode;

public record ErrorResponse(String code, String message, Object details) {
    public static ErrorResponse from(ErrorCode errorCode, Object details) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details);
    }
}
