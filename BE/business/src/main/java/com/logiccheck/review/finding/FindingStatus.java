package com.logiccheck.review.finding;

/**
 * 판정 상태. FE 의 VERDICT.PENDING 을 OPEN 으로 대체한다 (DEV3 F절).
 * OPEN ──ACCEPT──▶ ACCEPTED · OPEN ──REJECT──▶ REJECTED (DEV3 D-6).
 */
public enum FindingStatus {

    OPEN,
    ACCEPTED,
    REJECTED
}
