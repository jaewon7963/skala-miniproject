package com.logiccheck.review.annotation.dto;

import com.logiccheck.review.ReviewTimes;
import com.logiccheck.review.annotation.Annotation;
import com.logiccheck.review.annotation.dto.AnchorPayload.BBoxPayload;

import java.time.OffsetDateTime;

/**
 * 명세 24 · 25 · 26 응답.
 * source · color 필드가 없다 — finding_id 유무로 구분된다 (DEV3 D-7).
 * authorId 도 내리지 않는다 — MVP1 은 단독 사용자 검토라 표시할 곳이 없다.
 */
public record AnnotationResponse(
        String id,
        String jobId,
        String findingId,
        String body,
        AnchorPayload anchor,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static AnnotationResponse of(Annotation annotation) {
        return new AnnotationResponse(
                String.valueOf(annotation.getId()),
                String.valueOf(annotation.getJob().getId()),
                annotation.getFinding() == null ? null : String.valueOf(annotation.getFinding().getId()),
                annotation.getBody(),
                anchorOf(annotation),
                ReviewTimes.toResponse(annotation.getCreatedAt()),
                ReviewTimes.toResponse(annotation.getUpdatedAt())
        );
    }

    /** 좌표도 인용문도 없으면 anchor 자체를 내리지 않는다. */
    private static AnchorPayload anchorOf(Annotation annotation) {
        if (annotation.getPageNo() == null && annotation.getQuote() == null && !annotation.hasBbox()) {
            return null;
        }
        BBoxPayload bbox = annotation.hasBbox()
                ? new BBoxPayload(annotation.getBboxX(), annotation.getBboxY(),
                annotation.getBboxW(), annotation.getBboxH())
                : null;
        return new AnchorPayload(annotation.getPageNo(), annotation.getQuote(), bbox);
    }
}
