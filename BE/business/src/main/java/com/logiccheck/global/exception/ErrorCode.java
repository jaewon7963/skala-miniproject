package com.logiccheck.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "비밀번호는 8자 이상, 영문과 숫자를 각각 1자 이상 포함해야 합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),

    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "최대 50MB까지 업로드할 수 있습니다."),
    DUPLICATE_FILE(HttpStatus.CONFLICT, "이미 업로드한 파일입니다."),

    DOCUMENT_NOT_READY(HttpStatus.CONFLICT, "문서 파싱이 완료되지 않았습니다."),
    JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 분석이 진행 중입니다."),
    NO_REVIEW_JOB(HttpStatus.NOT_FOUND, "아직 분석하지 않은 문서입니다."),
    JOB_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 검토가 완료된 문서입니다."),
    EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
