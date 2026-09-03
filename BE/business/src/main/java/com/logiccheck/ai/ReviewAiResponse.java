package com.logiccheck.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 서버 → BE (명세 8-3 기준).
 *
 * 구 명세와 다른 점:
 * - findings[].type → findings[].severity 로 이름이 바뀌고 값이 ERROR · WARNING · INFO 다
 *   (DEV3 F절: FINDING_TYPE → SEVERITY). AI 팀과 확정이 필요하다. work_log.md 참고.
 * - findings[].method 는 받아도 무시한다. rule_id 유무로 파생하므로 AI 결과는 항상 RAG 다 (D-4).
 * - findings[].calculation 도 무시한다. RAG 면 null 이어야 한다 (D-4).
 * - evidence[].blockId 는 ERD 에 Block 테이블이 없어 쓰지 않는다.
 *
 * 모델에 강제하는 규칙 (명세 8-3):
 * 1. evidence 가 비어 있는 finding 은 반환 금지
 * 2. quote 는 원문에 존재하는 문자열 그대로. 요약·재작성 금지 (BE 가 대조 후 폐기)
 * 3. confidence 는 0~1 실수
 * 4. 사업 성공 가능성 판정 금지
 * 5. 출력은 JSON 단일 객체. 마크다운 코드펜스·설명 문장 금지
 */
public record ReviewAiResponse(
        String modelVersion,
        String promptVersion,
        String generatedAt,
        List<Finding> findings
) {

    public record Finding(
            String severity,
            String sectionId,
            Integer page,
            String title,
            String description,
            BigDecimal confidence,
            List<Evidence> evidence
    ) {
    }

    public record Evidence(String elementId, Integer page, String quote, String label) {
    }
}
