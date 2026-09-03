package com.logiccheck.review.job;

/** 사람의 검토 상태. status = DONE(AI 분석 완료)과 혼동하지 않는다 (DEV3 D-1). */
public enum ReviewStatus {

    IN_REVIEW,
    COMPLETED
}
