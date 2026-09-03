package com.logiccheck.document.dto;

import java.util.Map;

import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;

public enum DocumentSort {
    UPDATED_DESC, NAME_ASC;

    public static DocumentSort from(String value) {
        try {
            return DocumentSort.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "sort"));
        }
    }
}
