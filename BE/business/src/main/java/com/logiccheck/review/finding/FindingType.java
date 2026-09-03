package com.logiccheck.review.finding;

/** 검토 항목의 종류. 화면의 유형 배지·필터와 값이 1:1로 맞아야 한다. */
public enum FindingType {
    /** 오류 — 문서 안에서 값이 서로 어긋난다 */
    ERROR,
    /** 확인 필요 — 단정할 수 없어 사람이 봐야 한다 */
    NEEDS_CHECK,
    /** 근거 부족 — 주장은 있는데 뒷받침이 없다 */
    NO_EVIDENCE
}
