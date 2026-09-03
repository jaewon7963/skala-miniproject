package com.logiccheck.review.finding;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 검토 항목이 가리키는 원문 위치. {@code anchorId}가 원문 블록 id와 같아야 하이라이트가 붙는다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceResponse(String anchorId, int page, String label, String selectedText) {

    public static EvidenceResponse from(FindingEvidence evidence) {
        return new EvidenceResponse(evidence.getAnchorId(), evidence.getPageNo(), evidence.getLabel(),
                evidence.getSelectedText());
    }
}
