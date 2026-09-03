package com.logiccheck.review.finding;

/**
 * 검토사항의 심각도. FE 의 FINDING_TYPE(ERROR·NEEDS_CHECK·NO_EVIDENCE)을 대체한다 (DEV3 F절).
 * 목록 정렬 기준이므로 의미 순서를 order 로 들고 있다 — 문자열 정렬(ERROR·INFO·WARNING)과 다르다.
 */
public enum Severity {

    ERROR(0),
    WARNING(1),
    INFO(2);

    private final int order;

    Severity(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
