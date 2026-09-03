package com.logiccheck.review.job;

import java.util.Map;

import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;

public record CreateReviewJobRequest(String documentId) {

    public Long documentIdAsLong() {
        try {
            return Long.valueOf(documentId);
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "documentId"));
        }
    }
}
