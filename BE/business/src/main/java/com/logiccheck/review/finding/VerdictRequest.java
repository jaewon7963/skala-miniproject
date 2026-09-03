package com.logiccheck.review.finding;

import java.util.Map;

import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;

public record VerdictRequest(String verdict) {

    public Verdict toVerdict() {
        try {
            return Verdict.valueOf(verdict);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "verdict"));
        }
    }
}
