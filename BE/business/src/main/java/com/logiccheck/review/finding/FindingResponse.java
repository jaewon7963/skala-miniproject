package com.logiccheck.review.finding;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 검토 패널의 카드 한 장. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FindingResponse(
        String id,
        String type,
        int page,
        String sectionId,
        String title,
        String description,
        BigDecimal confidence,
        String method,
        String verdict,
        OffsetDateTime decidedAt,
        Calculation calculation,
        List<EvidenceResponse> evidence) {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
                String.valueOf(finding.getId()),
                finding.getFindingType().name(),
                finding.getPageNo(),
                finding.getSectionId() == null ? null : String.valueOf(finding.getSectionId()),
                finding.getTitle(),
                finding.getDescription(),
                finding.getConfidence(),
                finding.getMethod().name(),
                finding.getVerdict().name(),
                finding.getDecidedAt() == null ? null : finding.getDecidedAt().atOffset(KST),
                finding.getCalculation(),
                finding.getEvidence().stream().map(EvidenceResponse::from).toList());
    }
}
