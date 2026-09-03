// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

import java.util.Map;

/** 명세 1-2 실패 응답 — { code, message, details } */
public record ErrorResponse(String code, String message, Map<String, Object> details) {
}
