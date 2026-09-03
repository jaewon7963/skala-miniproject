package com.logiccheck.review.finding.dto;

import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingEvidence;
import com.logiccheck.review.finding.FindingMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * 명세 21 · 22 응답. 배열/객체를 래퍼 없이 그대로 반환한다 (명세 1-2).
 *
 * method 는 저장 컬럼이 아니라 rule_id 유무로 파생한다.
 * calculation 은 DETERMINISTIC 일 때만 채우고 RAG 면 null 이다.
 * embedding 은 응답에서 제외한다 — 엔티티에 아예 매핑하지 않았다. (DEV3 D-4)
 */
public record FindingResponse(
        String id,
        String jobId,
        String severity,
        String status,
        Integer page,
        String sectionId,
        String title,
        String description,
        BigDecimal confidence,
        String method,
        CalculationView calculation,
        List<EvidenceView> evidence
) {

    public static FindingResponse of(Finding finding) {
        FindingMethod method = finding.method();
        return new FindingResponse(
                String.valueOf(finding.getId()),
                String.valueOf(finding.getJob().getId()),
                finding.getSeverity().name(),
                finding.getStatus().name(),
                finding.getPageNo(),
                finding.getSectionId() == null ? null : String.valueOf(finding.getSectionId()),
                finding.getTitle(),
                finding.getDescription(),
                finding.getConfidence(),
                method.name(),
                method == FindingMethod.DETERMINISTIC ? CalculationView.of(finding) : null,
                finding.getEvidence().stream().map(EvidenceView::of).toList()
        );
    }

    /** 결정적 검산의 산출 근거. RAG 면 이 객체 자체가 null 이다. */
    public record CalculationView(String expression, String expected, String actual, String diff) {

        static CalculationView of(Finding finding) {
            if (finding.getCalcExpression() == null && finding.getCalcExpected() == null
                    && finding.getCalcActual() == null && finding.getCalcDiff() == null) {
                return null;
            }
            return new CalculationView(finding.getCalcExpression(), finding.getCalcExpected(),
                    finding.getCalcActual(), finding.getCalcDiff());
        }
    }

    /** id 가 FE 하이라이트 앵커 키다. quote 는 항상 채워진다 (DEV3 D-5). */
    public record EvidenceView(
            String id,
            Integer page,
            String label,
            String quote,
            BBoxView bbox,
            Integer charStart,
            Integer charEnd
    ) {

        static EvidenceView of(FindingEvidence evidence) {
            return new EvidenceView(
                    String.valueOf(evidence.getId()),
                    evidence.getPageNo(),
                    evidence.getLabel(),
                    evidence.getQuote(),
                    evidence.hasBbox() ? new BBoxView(evidence.getBboxX(), evidence.getBboxY(),
                            evidence.getBboxW(), evidence.getBboxH()) : null,
                    evidence.getCharStart(),
                    evidence.getCharEnd()
            );
        }
    }

    /** 페이지 크기 대비 0~1 상대 좌표. */
    public record BBoxView(BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h) {
    }
}
