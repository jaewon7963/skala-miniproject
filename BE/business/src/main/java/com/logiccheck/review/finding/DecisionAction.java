package com.logiccheck.review.finding;

/**
 * 판정 동작. finding_decisions.action 에 저장된다.
 * OPEN ──ACCEPT──▶ ACCEPTED · OPEN ──REJECT──▶ REJECTED (DEV3 D-6).
 *
 * 되돌리기(PENDING 복귀) 동작은 만들지 않는다 — FE 의 undoVerdict 버튼은 비활성화된다.
 */
public enum DecisionAction {

    ACCEPT(FindingStatus.ACCEPTED),
    REJECT(FindingStatus.REJECTED);

    private final FindingStatus resultStatus;

    DecisionAction(FindingStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public FindingStatus resultStatus() {
        return resultStatus;
    }
}
