// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

import com.logiccheck.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** 실패 응답은 { code, message, details } 로 통일한다 (명세 1-2). */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return body(e.getErrorCode(), e.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> details.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return body(ErrorCode.INVALID_REQUEST, details);
    }

    /** 경로 변수 타입 불일치 · 잘못된 JSON 본문은 400 이다. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception e) {
        return body(ErrorCode.INVALID_REQUEST, null);
    }

    /**
     * Spring MVC 가 스스로 던지는 예외는 org.springframework.web.ErrorResponse 를 구현한다.
     * (NoResourceFoundException 404 · HttpRequestMethodNotSupportedException 405 등).
     * 이들을 걸러내지 않으면 없는 경로가 500 으로 나간다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse mvcError) {
            int status = mvcError.getStatusCode().value();
            return ResponseEntity.status(status).body(toBody(byStatus(status), null));
        }
        log.error("처리하지 못한 예외", e);
        return body(ErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    private static ErrorCode byStatus(int status) {
        return switch (status) {
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            default -> status >= 500 ? ErrorCode.INTERNAL_SERVER_ERROR : ErrorCode.INVALID_REQUEST;
        };
    }

    private static ResponseEntity<ErrorResponse> body(ErrorCode code, Map<String, Object> details) {
        return ResponseEntity.status(HttpStatus.valueOf(code.getStatus())).body(toBody(code, details));
    }

    private static ErrorResponse toBody(ErrorCode code, Map<String, Object> details) {
        return new ErrorResponse(code.name(), code.getMessage(), details);
    }
}
