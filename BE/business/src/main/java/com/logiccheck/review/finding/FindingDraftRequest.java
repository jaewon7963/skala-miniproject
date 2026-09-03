package com.logiccheck.review.finding;

import java.util.List;
import java.util.Map;

import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;

/**
 * 사용자가 원문에서 직접 잡아 만든 검토 항목, 또는 질문 답변을 항목으로 승격한 것.
 *
 * <p>화면이 검토 항목 객체를 그대로 되돌려 보내므로 서버가 쓰지 않는 필드도 함께 온다.
 * 판정과 식별자는 서버가 정한다.
 */
public record FindingDraftRequest(
        String type,
        String method,
        Integer page,
        String sectionId,
        String title,
        String description,
        Double confidence,
        List<Evidence> evidence) {

    public record Evidence(String anchorId, Integer page, String label, String selectedText) {
    }

    public FindingType findingType() {
        try {
            return FindingType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "type"));
        }
    }

    public FindingMethod findingMethod() {
        try {
            return method == null ? FindingMethod.MANUAL : FindingMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return FindingMethod.MANUAL;
        }
    }

    public Long sectionIdAsLong() {
        try {
            return sectionId == null || sectionId.isBlank() ? null : Long.valueOf(sectionId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
