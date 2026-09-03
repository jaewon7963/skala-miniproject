package com.logiccheck.ai;

/** AI 호출 실패. 파이프라인이 잡아 review_jobs.error_code 로 옮긴다. */
public class ReviewAiException extends RuntimeException {

    private final String errorCode;

    public ReviewAiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReviewAiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
