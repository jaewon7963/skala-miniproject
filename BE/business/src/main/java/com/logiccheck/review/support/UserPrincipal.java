// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

/** JWT 인증 주체. 개발자1 구현이 오면 global/security 의 동명 타입으로 교체한다. */
public record UserPrincipal(Long userId) {
}
