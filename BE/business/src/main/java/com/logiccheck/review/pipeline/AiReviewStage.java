package com.logiccheck.review.pipeline;

import com.logiccheck.ai.ReviewAiClient;
import com.logiccheck.ai.ReviewAiException;
import com.logiccheck.ai.ReviewAiProperties;
import com.logiccheck.ai.ReviewAiRequest;
import com.logiccheck.ai.ReviewAiResponse;
import com.logiccheck.ai.ReviewAiResponseValidator;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.document.port.DocumentStructurePort.SectionView;
import com.logiccheck.review.finding.Severity;
import com.logiccheck.review.pipeline.FindingDraft.EvidenceDraft;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 관계 판단 (DEV3 D-10 4·5단계).
 *
 * review.ai.enabled=false 면 아무 것도 하지 않는다 — AI 서버 없이도 나머지 파이프라인이 돌아야 한다.
 * enabled=true 인데 호출이나 스키마 검증이 실패하면 ReviewAiException 을 던져
 * 파이프라인이 status = FAILED + error_code 로 남긴다 (DEV3 D-10 §8.10).
 *
 * rule_id 를 채우지 않으므로 여기서 만든 항목은 항상 method = RAG 로 파생된다 (D-4).
 * calculation 도 채우지 않는다.
 */
@Component
public class AiReviewStage {

    /** 명세 8-2 의 criteria. 설정으로 바꿀 수 있게 두되 기본값은 4종이다. */
    private static final List<String> CRITERIA =
            List.of("NUMERIC_CONSISTENCY", "CLAIM_EVIDENCE", "TECH_KPI_FIT", "CROSS_ITEM_CONFLICT");

    private final ReviewAiProperties properties;
    private final ObjectProvider<ReviewAiClient> clientProvider;
    private final ReviewAiResponseValidator validator;

    public AiReviewStage(ReviewAiProperties properties,
                         ObjectProvider<ReviewAiClient> clientProvider,
                         ReviewAiResponseValidator validator) {
        this.properties = properties;
        this.clientProvider = clientProvider;
        this.validator = validator;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public List<FindingDraft> run(Long jobId, String documentTitle,
                                  List<SectionView> sections, List<ElementView> elements) {
        if (!properties.enabled()) {
            return List.of();
        }
        ReviewAiClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new ReviewAiException("AI_CLIENT_MISSING",
                    "review.ai.enabled=true 인데 ReviewAiClient 빈이 없다.");
        }

        ReviewAiResponse response = client.review(toRequest(jobId, documentTitle, sections, elements));
        Map<String, ElementView> elementById = elements.stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.id()), Function.identity(),
                        (left, right) -> left));

        return validator.validate(response).stream()
                .map(finding -> toDraft(finding, elementById))
                .toList();
    }

    private ReviewAiRequest toRequest(Long jobId, String documentTitle,
                                      List<SectionView> sections, List<ElementView> elements) {
        return new ReviewAiRequest(
                String.valueOf(jobId),
                documentTitle,
                properties.model(),
                properties.temperature(),
                properties.promptVersion(),
                CRITERIA,
                sections.stream()
                        .map(s -> new ReviewAiRequest.Section(String.valueOf(s.id()), s.title(), s.pageNo()))
                        .toList(),
                elements.stream()
                        .map(e -> new ReviewAiRequest.Element(String.valueOf(e.id()), e.kind(), null,
                                e.pageNo(), e.rawText()))
                        .toList()
        );
    }

    /**
     * bbox 는 AI 응답에서 받지 않는다. elementId 로 추출 요소를 되짚어 그 좌표를 쓴다 —
     * 좌표의 출처를 파싱 결과로 한정해 AI 가 좌표를 지어내지 못하게 한다 (DEV3 D-5).
     */
    private static FindingDraft toDraft(ReviewAiResponse.Finding finding,
                                        Map<String, ElementView> elementById) {
        List<EvidenceDraft> evidence = finding.evidence().stream()
                .map(item -> toEvidence(item, elementById.get(item.elementId())))
                .toList();
        List<Long> elementIds = finding.evidence().stream()
                .map(item -> elementById.get(item.elementId()))
                .filter(java.util.Objects::nonNull)
                .map(ElementView::id)
                .distinct()
                .toList();

        return new FindingDraft(
                null,                                   // rule_id 없음 → method = RAG
                Severity.valueOf(finding.severity()),
                finding.title(),
                finding.description(),
                finding.confidence(),
                finding.page(),
                parseSectionId(finding.sectionId()),
                null,                                   // RAG 면 calculation 은 null (D-4)
                evidence,
                elementIds
        );
    }

    private static EvidenceDraft toEvidence(ReviewAiResponse.Evidence item, ElementView element) {
        var bbox = element == null ? null : element.bbox();
        return new EvidenceDraft(
                item.page(),
                item.quote(),
                item.label(),
                bbox == null ? null : java.math.BigDecimal.valueOf(bbox.x()),
                bbox == null ? null : java.math.BigDecimal.valueOf(bbox.y()),
                bbox == null ? null : java.math.BigDecimal.valueOf(bbox.w()),
                bbox == null ? null : java.math.BigDecimal.valueOf(bbox.h()),
                null,
                null
        );
    }

    private static Long parseSectionId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
