package com.logiccheck.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AI 응답 스키마 검증 (DEV3 D-10 §8.10, docs/ai/schema-finding.json). */
class ReviewAiResponseValidatorTest {

    private final ReviewAiResponseValidator validator = new ReviewAiResponseValidator();

    @Test
    void 규격에_맞는_항목은_통과한다() {
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.96", evidence(11, "인용문")))))
                .hasSize(1);
    }

    @Test
    void findings_가_없으면_응답_전체를_거부한다() {
        assertThatThrownBy(() -> validator.validate(
                new ReviewAiResponse("m", "p", "t", null)))
                .isInstanceOf(ReviewAiException.class)
                .extracting(e -> ((ReviewAiException) e).getErrorCode())
                .isEqualTo("AI_RESPONSE_INVALID");
    }

    @Test
    void null_응답도_거부한다() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ReviewAiException.class);
    }

    @Test
    void 규격을_벗어난_항목만_버리고_나머지는_살린다() {
        ReviewAiResponse response = response(
                finding("ERROR", "정상", "0.90", evidence(11, "인용문")),
                finding("CRITICAL", "잘못된 severity", "0.90", evidence(11, "인용문")),
                finding("INFO", "정상 2", "0.10", evidence(11, "인용문")));

        assertThat(validator.validate(response))
                .extracting(ReviewAiResponse.Finding::title)
                .containsExactly("정상", "정상 2");
    }

    @Test
    void severity_가_ERROR_WARNING_INFO_가_아니면_버린다() {
        for (String severity : List.of("NEEDS_CHECK", "NO_EVIDENCE", "high", "")) {
            assertThat(validator.validate(response(finding(severity, "제목", "0.9", evidence(1, "q")))))
                    .as("severity = %s", severity)
                    .isEmpty();
        }
        assertThat(validator.validate(response(finding(null, "제목", "0.9", evidence(1, "q"))))).isEmpty();
    }

    @Test
    void title_이_비면_버린다() {
        assertThat(validator.validate(response(finding("ERROR", "   ", "0.9", evidence(1, "q"))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", null, "0.9", evidence(1, "q"))))).isEmpty();
    }

    @Test
    void confidence_가_0에서_1_밖이면_버린다() {
        assertThat(validator.validate(response(finding("ERROR", "제목", "1.01", evidence(1, "q"))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", "-0.01", evidence(1, "q"))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", null, evidence(1, "q"))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", "0", evidence(1, "q"))))).hasSize(1);
        assertThat(validator.validate(response(finding("ERROR", "제목", "1", evidence(1, "q"))))).hasSize(1);
    }

    @Test
    void 근거가_없는_항목은_버린다() {
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.9")))).isEmpty();
        assertThat(validator.validate(response(
                new ReviewAiResponse.Finding("ERROR", null, 11, "제목", "설명",
                        new BigDecimal("0.9"), null)))).isEmpty();
    }

    @Test
    void 인용문이_비었거나_페이지가_잘못된_근거는_항목째로_버린다() {
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.9", evidence(11, "  "))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.9", evidence(11, null))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.9", evidence(0, "q"))))).isEmpty();
        assertThat(validator.validate(response(finding("ERROR", "제목", "0.9", evidence(null, "q"))))).isEmpty();
    }

    @Test
    void 근거가_여러_건이면_하나라도_어긋나면_버린다() {
        assertThat(validator.validate(response(
                finding("ERROR", "제목", "0.9", evidence(11, "정상"), evidence(11, "")))))
                .isEmpty();
    }

    @Test
    void API_Key_는_설정_문자열에_노출되지_않는다() {
        ReviewAiProperties properties = new ReviewAiProperties(true, "https://ai.example",
                "/v1/review", "sk-super-secret-value", "model-x", 0.2, "review-v1", null, null);

        assertThat(properties.toString())
                .doesNotContain("sk-super-secret-value")
                .contains("***");
    }

    private static ReviewAiResponse response(ReviewAiResponse.Finding... findings) {
        return new ReviewAiResponse("m", "p", "t", Arrays.asList(findings));
    }

    private static ReviewAiResponse.Finding finding(String severity, String title, String confidence,
                                                    ReviewAiResponse.Evidence... evidence) {
        return new ReviewAiResponse.Finding(severity, null, 11, title, "설명",
                confidence == null ? null : new BigDecimal(confidence),
                evidence.length == 0 ? List.of() : Arrays.asList(evidence));
    }

    private static ReviewAiResponse.Evidence evidence(Integer page, String quote) {
        return new ReviewAiResponse.Evidence("901", page, quote, "라벨");
    }
}
