package com.logiccheck.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AI 응답 스키마 검증 (DEV3 D-10 §8.10).
 * "AI 응답을 신뢰 데이터처럼 바로 저장하지 않는다. JSON Schema 검증 + 인용문 원문 대조를 거친다."
 *
 * 스키마 문서는 docs/ai/schema-finding.json 이다. 여기서는 같은 규칙을 코드로 강제한다 —
 * 검증 라이브러리를 새로 넣지 않기 위함이다 (DEV3 A-8: 의존성 추가 금지).
 *
 * 응답 자체가 규격을 벗어나면(findings 누락) 전체를 거부한다.
 * 개별 항목이 규격을 벗어나면 그 항목만 버리고 나머지는 살린다.
 */
@Component
public class ReviewAiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(ReviewAiResponseValidator.class);
    private static final Set<String> SEVERITIES = Set.of("ERROR", "WARNING", "INFO");
    private static final BigDecimal ONE = BigDecimal.ONE;

    /** @throws ReviewAiException 응답 전체가 규격을 벗어난 경우 */
    public List<ReviewAiResponse.Finding> validate(ReviewAiResponse response) {
        if (response == null || response.findings() == null) {
            throw new ReviewAiException("AI_RESPONSE_INVALID", "AI 응답에 findings 가 없다.");
        }

        List<ReviewAiResponse.Finding> valid = new ArrayList<>();
        for (ReviewAiResponse.Finding finding : response.findings()) {
            String reason = reject(finding);
            if (reason != null) {
                log.info("규격을 벗어난 AI 항목을 버린다. 사유={} title={}", reason,
                        finding == null ? null : finding.title());
                continue;
            }
            valid.add(finding);
        }
        return valid;
    }

    /** 통과하면 null, 버려야 하면 사유 문자열. */
    private static String reject(ReviewAiResponse.Finding finding) {
        if (finding == null) {
            return "항목이 null";
        }
        if (finding.severity() == null || !SEVERITIES.contains(finding.severity())) {
            return "severity 가 ERROR·WARNING·INFO 가 아님";
        }
        if (finding.title() == null || finding.title().isBlank()) {
            return "title 이 비어 있음";
        }
        BigDecimal confidence = finding.confidence();
        if (confidence == null
                || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(ONE) > 0) {
            return "confidence 가 0~1 이 아님";
        }
        // 명세 8-3 규칙 1: evidence 가 비어 있는 finding 은 반환 금지
        if (finding.evidence() == null || finding.evidence().isEmpty()) {
            return "evidence 가 없음";
        }
        for (ReviewAiResponse.Evidence evidence : finding.evidence()) {
            if (evidence == null) {
                return "evidence 항목이 null";
            }
            if (evidence.page() == null || evidence.page() < 1) {
                return "evidence.page 가 1 이상이 아님";
            }
            if (evidence.quote() == null || evidence.quote().isBlank()) {
                return "evidence.quote 가 비어 있음";
            }
        }
        return null;
    }
}
