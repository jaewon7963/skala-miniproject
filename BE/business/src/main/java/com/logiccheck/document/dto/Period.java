package com.logiccheck.document.dto;

import java.time.Instant;
import java.util.Map;

import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;

public enum Period {
    ALL, D7, D30, D90;

    public static Period from(String value) {
        try {
            return Period.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "period"));
        }
    }

    public Instant cutoff() {
        Instant now = Instant.now();
        return switch (this) {
            case ALL -> null;
            case D7 -> now.minusSeconds(7L * 86400);
            case D30 -> now.minusSeconds(30L * 86400);
            case D90 -> now.minusSeconds(90L * 86400);
        };
    }
}
