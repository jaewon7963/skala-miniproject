package com.logiccheck.review.pipeline;

import com.logiccheck.review.finding.Severity;

import java.math.BigDecimal;
import java.util.List;

/**
 * 저장 전 검토사항. 결정적 검산과 AI 판단이 모두 이 형태로 결과를 낸다.
 *
 * ruleId 가 있으면 method = DETERMINISTIC, 없으면 RAG 로 파생된다 (DEV3 D-4).
 * evidence 가 비면 저장하지 않는다 — 근거 없는 지적을 반환하지 않는 것이 전제다 (DEV3 D-5).
 */
public record FindingDraft(
        Long ruleId,
        Severity severity,
        String title,
        String description,
        BigDecimal confidence,
        Integer pageNo,
        Long sectionId,
        Calculation calculation,
        List<EvidenceDraft> evidence,
        List<Long> elementIds
) {

    public boolean hasEvidence() {
        return evidence != null && !evidence.isEmpty();
    }

    public record Calculation(String expression, String expected, String actual, String diff) {
    }

    public record EvidenceDraft(
            int pageNo,
            String quote,
            String label,
            BigDecimal bboxX,
            BigDecimal bboxY,
            BigDecimal bboxW,
            BigDecimal bboxH,
            Integer charStart,
            Integer charEnd
    ) {
    }
}
