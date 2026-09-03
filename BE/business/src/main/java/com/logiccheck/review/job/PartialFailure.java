package com.logiccheck.review.job;

/** 일부 페이지를 건너뛰고 진행했을 때 사용자에게 알리는 사유. 페이지당 한 건만 남긴다. */
public record PartialFailure(int page, String reason) {
}
