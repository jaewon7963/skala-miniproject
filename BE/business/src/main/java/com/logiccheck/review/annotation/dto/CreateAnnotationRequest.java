package com.logiccheck.review.annotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 명세 25 요청.
 * authorId 를 받지 않는다 — JWT 사용자로 고정한다 (DEV3 D-7).
 * source · color 필드도 없다.
 */
public record CreateAnnotationRequest(
        String findingId,
        @NotBlank String body,
        @Valid AnchorPayload anchor
) {
}
