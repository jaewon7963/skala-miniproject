package com.logiccheck.document.service;

import com.logiccheck.document.entity.ParseStatus;
import com.logiccheck.review.port.ReviewJobQueryPort.LatestJobView;

/**
 * 문서 목록·상세에 찍히는 상태 배지 값을 계산한다.
 *
 * <p>파싱 상태와 분석 작업 상태 두 축을 화면이 이해하는 한 가지 값으로 합친다.
 * 값은 화면의 상태 필터·배지와 1:1로 맞아야 하므로 임의로 늘리지 않는다.
 */
public final class DisplayStatusCalculator {

    public static final String IDLE = "IDLE";
    public static final String PARSING = "PARSING";
    public static final String ANALYZING = "ANALYZING";
    public static final String REVIEWING = "REVIEWING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    private DisplayStatusCalculator() {
    }

    public static String calculate(ParseStatus parseStatus, LatestJobView job) {
        if (parseStatus == ParseStatus.FAILED) {
            return FAILED;
        }
        if (parseStatus != ParseStatus.DONE) {
            return PARSING;
        }
        if (job == null) {
            return IDLE;
        }
        return switch (job.status()) {
            case "PENDING", "RUNNING" -> ANALYZING;
            case "FAILED" -> FAILED;
            default -> "COMPLETED".equals(job.reviewStatus()) ? DONE : REVIEWING;
        };
    }
}
