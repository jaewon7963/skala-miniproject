package com.logiccheck.review.job;

import java.util.EnumSet;
import java.util.Set;

/**
 * AI 분석의 진행 상태. 사람의 검토 상태(ReviewStatus)와 서로 독립이다 (DEV3 D-1).
 * ERD 에 PARTIAL 이 없고 FE 도 JOB_STATUS.PARTIAL 을 제거했으므로 추가하지 않는다 (미결 #9).
 */
public enum JobStatus {

    PENDING,
    RUNNING,
    DONE,
    FAILED;

    /** 같은 문서에 새 Job 을 만들 수 없는 상태. DB 부분 유니크 인덱스와 동일한 집합이다. */
    public static final Set<JobStatus> ACTIVE = EnumSet.of(PENDING, RUNNING);

    /** FE 폴링 중단 신호. review_status 와 무관하다 (DEV3 D-1). */
    public boolean isTerminal() {
        return this == DONE || this == FAILED;
    }
}
