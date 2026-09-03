package com.logiccheck.review.finding;

/** 그 항목을 어떻게 찾아냈는지. 화면이 배지로 노출한다. */
public enum FindingMethod {
    /** 계산으로 확인한 것 (표 합계 재계산 등) */
    DETERMINISTIC,
    /** 문맥 판단으로 찾은 것 */
    RAG,
    /** 사용자가 원문에서 직접 추가한 것 */
    MANUAL
}
