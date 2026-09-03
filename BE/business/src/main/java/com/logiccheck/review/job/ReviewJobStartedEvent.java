package com.logiccheck.review.job;

/** 분석 작업이 커밋된 뒤 백그라운드 실행을 깨우는 신호. */
public record ReviewJobStartedEvent(Long jobId, Long documentId, Long ownerId, int parseDurationMs,
                                     int analyzeDurationMs) {
}
