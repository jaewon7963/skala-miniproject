package com.logiccheck.review.job.dto;

import java.util.Map;

/**
 * 명세 17 의 summary. status = DONE 일 때만 채우고 그 외에는 null 이다 (DEV3 D-3).
 * 별도 스냅샷을 저장하지 않고 조회 시 계산한다. decided = accepted + rejected.
 */
public record JobSummaryView(
        long total,
        Map<String, Long> bySeverity,
        long decided,
        long accepted,
        long rejected,
        long open
) {
}
