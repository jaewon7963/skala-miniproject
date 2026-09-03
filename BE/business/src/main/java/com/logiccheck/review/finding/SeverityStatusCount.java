package com.logiccheck.review.finding;

/** summary 집계용 프로젝션. */
public record SeverityStatusCount(Severity severity, FindingStatus status, Long count) {
}
