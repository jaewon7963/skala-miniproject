package com.logiccheck.ai;

import java.util.List;

/** 모델이 돌려주는 분석 결과. */
public record ReviewAiResult(String modelVersion, String promptVersion, List<AiFinding> findings) {

    /**
     * 검토 항목 후보 한 건.
     *
     * <p>{@code evidence}가 비어 있으면 저장하지 않는다. 어디를 보라는 것인지 못 알려주는
     * 지적은 화면에서 확인할 방법이 없기 때문이다.
     */
    public record AiFinding(
            String type,
            String method,
            Long sectionId,
            int page,
            String title,
            String description,
            double confidence,
            AiCalculation calculation,
            List<AiEvidence> evidence) {
    }

    public record AiEvidence(String anchorId, int page, String label) {
    }

    /** 검산 근거. 표시용 문자열이 아니라 숫자로 넘긴다 — 저장하는 쪽이 숫자 컬럼이다. */
    public record AiCalculation(String expression, Double expected, Double actual) {
    }
}
