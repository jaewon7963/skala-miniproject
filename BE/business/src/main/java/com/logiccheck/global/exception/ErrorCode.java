package com.logiccheck.global.exception;

/**
 * 명세 1-3 / DEV3 A-4. Day 0 에 전량 정의하고 이후 아무도 이 파일을 수정하지 않는다.
 * 새 코드가 필요하면 추가하지 말고 팀에 먼저 묻는다.
 */
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(400, "요청 값을 확인해주세요."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "요청하신 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 개발자1 — 회원/인증
    EMAIL_ALREADY_EXISTS(409, "이미 가입된 이메일입니다."),
    INVALID_PASSWORD(422, "비밀번호는 8자 이상, 영문과 숫자를 각각 1자 이상 포함해야 합니다."),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호를 확인해주세요."),
    INVALID_REFRESH_TOKEN(401, "다시 로그인해주세요."),

    // 개발자2 — 문서
    UNSUPPORTED_FILE_TYPE(415, "PDF 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(413, "최대 50MB까지 업로드할 수 있습니다."),
    DUPLICATE_FILE(409, "이미 업로드한 파일입니다."),

    // 개발자3 — 분석/검토
    DOCUMENT_NOT_READY(409, "문서 파싱이 완료되지 않았습니다."),
    JOB_ALREADY_RUNNING(409, "이미 분석이 진행 중입니다."),
    NO_REVIEW_JOB(404, "아직 분석하지 않은 문서입니다."),
    JOB_ALREADY_COMPLETED(409, "이미 검토가 완료된 문서입니다."),
    EXPORT_FAILED(500, "PDF 생성에 실패했습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
