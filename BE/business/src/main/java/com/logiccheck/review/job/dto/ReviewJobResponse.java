package com.logiccheck.review.job.dto;

import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.review.ReviewTimes;
import com.logiccheck.review.job.ReviewJob;

import java.time.OffsetDateTime;

/**
 * 명세 17 · 18 응답. 래퍼로 감싸지 않고 이 객체를 그대로 반환한다 (명세 1-2).
 *
 * documentTitle · pageCount 는 개발자2의 documents 에서 오므로 DocumentQueryPort 로 조합한다 (DEV3 D-3).
 * 응답에서 제외: reviewScore(산식 미확정) · steps · parseProgress · analyzeProgress ·
 * partialFailures · discovered (ERD 에 컬럼 없음).
 */
public record ReviewJobResponse(
        String id,
        String documentId,
        String documentTitle,
        Integer pageCount,
        String status,
        String reviewStatus,
        boolean terminal,
        String rulesetVersion,
        String errorCode,
        JobSummaryView summary,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime completedAt
) {

    public static ReviewJobResponse of(ReviewJob job, DocumentMetaView meta, JobSummaryView summary) {
        return new ReviewJobResponse(
                String.valueOf(job.getId()),
                String.valueOf(job.getDocumentId()),
                meta.title(),
                meta.pageCount(),
                job.getStatus().name(),
                job.getReviewStatus().name(),
                job.getStatus().isTerminal(),
                job.getRulesetVersion(),
                job.getErrorCode(),
                summary,
                ReviewTimes.toResponse(job.getStartedAt()),
                ReviewTimes.toResponse(job.getFinishedAt()),
                ReviewTimes.toResponse(job.getCompletedAt())
        );
    }
}
