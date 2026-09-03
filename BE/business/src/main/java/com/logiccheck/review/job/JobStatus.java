package com.logiccheck.review.job;

/** AI 분석의 진행 상태. 사람의 검토 진행 상태({@link ReviewStatus})와는 별개다. */
public enum JobStatus {
    PENDING, RUNNING, PARTIAL, DONE, FAILED;

    /** 화면의 폴링 루프가 멈추는 조건. PARTIAL은 종료가 아니다. */
    public boolean isTerminal() {
        return this == DONE || this == FAILED;
    }
}
