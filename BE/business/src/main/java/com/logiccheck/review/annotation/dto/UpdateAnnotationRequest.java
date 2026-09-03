package com.logiccheck.review.annotation.dto;

import jakarta.validation.constraints.NotBlank;

/** 명세 26 요청. 본문만 수정한다. */
public record UpdateAnnotationRequest(@NotBlank String body) {
}
