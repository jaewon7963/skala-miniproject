package com.logiccheck.review.job;

/** 검토 화면 좌측 목차 한 줄. {@code findingCount}는 그 섹션에 걸린 검토 항목 수다. */
public record OutlineResponse(String id, String title, int level, Integer page, long findingCount) {
}
