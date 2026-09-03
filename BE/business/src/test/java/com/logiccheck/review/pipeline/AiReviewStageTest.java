package com.logiccheck.review.pipeline;

import com.logiccheck.ai.ReviewAiClient;
import com.logiccheck.ai.ReviewAiException;
import com.logiccheck.ai.ReviewAiProperties;
import com.logiccheck.ai.ReviewAiRequest;
import com.logiccheck.ai.ReviewAiResponse;
import com.logiccheck.ai.ReviewAiResponseValidator;
import com.logiccheck.document.port.DocumentStructurePort.BBox;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.document.port.DocumentStructurePort.SectionView;
import com.logiccheck.review.finding.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AI 관계 판단 (DEV3 D-10 4·5단계). */
class AiReviewStageTest {

    private static final List<SectionView> SECTIONS =
            List.of(new SectionView(5L, null, "매출 · 재무 계획", 11, 5, "ORIGINAL"));
    private static final List<ElementView> ELEMENTS = List.of(
            new ElementView(901L, 11, "NUMBER", "2027년 예상 매출 24억 원",
                    new BigDecimal("24"), "억 원", new BBox(0.12, 0.31, 0.66, 0.03)));

    @Test
    void 비활성이면_AI_를_호출하지_않는다() {
        AiReviewStage stage = stage(properties(false), request -> {
            throw new AssertionError("호출되어서는 안 된다");
        });

        assertThat(stage.isEnabled()).isFalse();
        assertThat(stage.run(42L, "문서", SECTIONS, ELEMENTS)).isEmpty();
    }

    @Test
    void AI_항목은_rule_id_가_없어_RAG_로_파생되고_calculation_이_null_이다() {
        AiReviewStage stage = stage(properties(true), request -> response("WARNING", "0.780"));

        List<FindingDraft> drafts = stage.run(42L, "문서", SECTIONS, ELEMENTS);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).ruleId()).isNull();
        assertThat(drafts.get(0).calculation()).isNull();
        assertThat(drafts.get(0).severity()).isEqualTo(Severity.WARNING);
        assertThat(drafts.get(0).confidence()).isEqualByComparingTo("0.780");
    }

    @Test
    void bbox_는_AI_응답이_아니라_elementId_로_되짚은_파싱_결과에서_온다() {
        AiReviewStage stage = stage(properties(true), request -> response("ERROR", "0.900"));

        FindingDraft draft = stage.run(42L, "문서", SECTIONS, ELEMENTS).get(0);

        FindingDraft.EvidenceDraft evidence = draft.evidence().get(0);
        assertThat(evidence.bboxX()).isEqualByComparingTo("0.12");
        assertThat(evidence.bboxY()).isEqualByComparingTo("0.31");
        assertThat(draft.elementIds()).containsExactly(901L);
    }

    @Test
    void 모르는_elementId_는_좌표_없이_남는다() {
        AiReviewStage stage = stage(properties(true), request -> new ReviewAiResponse("m", "p", "t",
                List.of(new ReviewAiResponse.Finding("INFO", null, 11, "제목", "설명",
                        new BigDecimal("0.5"),
                        List.of(new ReviewAiResponse.Evidence("999", 11, "2027년 예상 매출 24억 원", null))))));

        FindingDraft draft = stage.run(42L, "문서", SECTIONS, ELEMENTS).get(0);

        assertThat(draft.evidence().get(0).bboxX()).isNull();
        assertThat(draft.evidence().get(0).quote()).isEqualTo("2027년 예상 매출 24억 원");
        assertThat(draft.elementIds()).isEmpty();
    }

    @Test
    void 요청에는_설정된_모델_과_temperature_와_promptVersion_이_담긴다() {
        ReviewAiRequest[] captured = new ReviewAiRequest[1];
        AiReviewStage stage = stage(properties(true), request -> {
            captured[0] = request;
            return response("ERROR", "0.9");
        });

        stage.run(42L, "AI 매장 안내 로봇 사업계획서", SECTIONS, ELEMENTS);

        assertThat(captured[0].jobId()).isEqualTo("42");
        assertThat(captured[0].documentTitle()).isEqualTo("AI 매장 안내 로봇 사업계획서");
        assertThat(captured[0].model()).isEqualTo("model-x");
        assertThat(captured[0].temperature()).isEqualTo(0.2);
        assertThat(captured[0].promptVersion()).isEqualTo("review-v1");
        assertThat(captured[0].criteria()).contains("NUMERIC_CONSISTENCY", "CLAIM_EVIDENCE");
        assertThat(captured[0].elements()).hasSize(1);
        assertThat(captured[0].elements().get(0).id()).isEqualTo("901");
    }

    @Test
    void 클라이언트_빈이_없으면_AI_CLIENT_MISSING_이다() {
        AiReviewStage stage = new AiReviewStage(properties(true), emptyProvider(),
                new ReviewAiResponseValidator());

        assertThatThrownBy(() -> stage.run(42L, "문서", SECTIONS, ELEMENTS))
                .isInstanceOf(ReviewAiException.class)
                .extracting(e -> ((ReviewAiException) e).getErrorCode())
                .isEqualTo("AI_CLIENT_MISSING");
    }

    @Test
    void 호출_실패는_그대로_전파되어_파이프라인이_FAILED_로_남긴다() {
        AiReviewStage stage = stage(properties(true), request -> {
            throw new ReviewAiException("AI_CALL_FAILED", "타임아웃");
        });

        assertThatThrownBy(() -> stage.run(42L, "문서", SECTIONS, ELEMENTS))
                .isInstanceOf(ReviewAiException.class)
                .extracting(e -> ((ReviewAiException) e).getErrorCode())
                .isEqualTo("AI_CALL_FAILED");
    }

    private static ReviewAiResponse response(String severity, String confidence) {
        return new ReviewAiResponse("m", "review-v1", "t",
                List.of(new ReviewAiResponse.Finding(severity, "5", 11, "제목", "설명",
                        new BigDecimal(confidence),
                        List.of(new ReviewAiResponse.Evidence("901", 11,
                                "2027년 예상 매출 24억 원", "p.11")))));
    }

    private static ReviewAiProperties properties(boolean enabled) {
        return new ReviewAiProperties(enabled, "https://ai.example", "/v1/review",
                "secret", "model-x", 0.2, "review-v1", null, null);
    }

    private static AiReviewStage stage(ReviewAiProperties properties, ReviewAiClient client) {
        return new AiReviewStage(properties, singletonProvider(client), new ReviewAiResponseValidator());
    }

    private static ObjectProvider<ReviewAiClient> singletonProvider(ReviewAiClient client) {
        return new ObjectProvider<>() {
            @Override
            public ReviewAiClient getObject() {
                return client;
            }

            @Override
            public ReviewAiClient getObject(Object... args) {
                return client;
            }

            @Override
            public ReviewAiClient getIfAvailable() {
                return client;
            }

            @Override
            public ReviewAiClient getIfUnique() {
                return client;
            }
        };
    }

    private static ObjectProvider<ReviewAiClient> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public ReviewAiClient getObject() {
                throw new IllegalStateException("no bean");
            }

            @Override
            public ReviewAiClient getObject(Object... args) {
                throw new IllegalStateException("no bean");
            }

            @Override
            public ReviewAiClient getIfAvailable() {
                return null;
            }

            @Override
            public ReviewAiClient getIfUnique() {
                return null;
            }
        };
    }
}
