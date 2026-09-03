package com.logiccheck.review.job.dto;

import jakarta.validation.constraints.NotBlank;

/** 명세 16 요청. ID 는 요청·응답에서 문자열이다 (명세 1-2). */
public record CreateReviewJobRequest(@NotBlank String documentId) {
}
