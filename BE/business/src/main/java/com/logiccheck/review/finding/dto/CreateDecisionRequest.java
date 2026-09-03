package com.logiccheck.review.finding.dto;

import com.logiccheck.review.finding.DecisionAction;
import jakarta.validation.constraints.NotNull;

/**
 * 명세 23 요청.
 *
 * annotationBody 는 미결 #5 를 "통합" 으로 확정한 결과다. 값이 있으면 판정과 주석을
 * 하나의 트랜잭션에서 저장한다. 비우면 판정만 한다. 명세 25 도 그대로 남아 있으므로
 * FE 는 둘 중 아무 방식이나 쓸 수 있다.
 *
 * actorId 를 받지 않는다 — JWT 사용자로 고정한다.
 */
public record CreateDecisionRequest(
        @NotNull DecisionAction action,
        String note,
        String annotationBody
) {
}
