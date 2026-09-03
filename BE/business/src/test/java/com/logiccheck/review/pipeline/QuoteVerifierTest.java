package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort.PageView;
import com.logiccheck.review.finding.Severity;
import com.logiccheck.review.pipeline.FindingDraft.EvidenceDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 인용문 검증 (DEV3 D-5). 원문과 불일치하는 항목은 저장하지 않는다. */
class QuoteVerifierTest {

    private final QuoteVerifier verifier = new QuoteVerifier();

    private static final List<PageView> PAGES = List.of(
            new PageView(11, 595.0, 842.0, "5. 매출 · 재무 계획\n2027년 예상 매출 24억 원, 영업이익률 18%."),
            new PageView(14, 595.0, 842.0, null)   // 이미지형 표 — 텍스트 레이어 없음
    );

    @Test
    void 원문에_있는_인용문은_통과한다() {
        assertThat(verifier.of(PAGES).accepts(draft(11, "2027년 예상 매출 24억 원"), false)).isTrue();
    }

    @Test
    void 공백_차이는_무시한다() {
        assertThat(verifier.of(PAGES).accepts(draft(11, "2027년  예상   매출 24억원"), false)).isTrue();
    }

    @Test
    void 원문에_없는_인용문은_폐기한다() {
        assertThat(verifier.of(PAGES).accepts(draft(11, "2027년 예상 매출 30억 원"), false)).isFalse();
    }

    @Test
    void 요약하거나_재작성한_인용문은_폐기한다() {
        assertThat(verifier.of(PAGES).accepts(draft(11, "매출 목표는 24억 원 수준"), false)).isFalse();
    }

    @Test
    void 근거가_없는_초안은_폐기한다() {
        FindingDraft noEvidence = new FindingDraft(null, Severity.ERROR, "제목", "설명",
                new BigDecimal("0.9"), 11, null, null, List.of(), List.of());

        assertThat(verifier.of(PAGES).accepts(noEvidence, true)).isFalse();
    }

    @Test
    void 인용문이_비어_있으면_폐기한다() {
        assertThat(verifier.of(PAGES).accepts(draft(11, "   "), true)).isFalse();
    }

    @Test
    void 원문_텍스트가_없는_페이지는_결정적_검산_결과만_남긴다() {
        FindingDraft draft = draft(14, "이미지 표의 수치");

        assertThat(verifier.of(PAGES).accepts(draft, true)).as("결정적 검산 — 신뢰").isTrue();
        assertThat(verifier.of(PAGES).accepts(draft, false)).as("AI 결과 — 폐기").isFalse();
    }

    @Test
    void 없는_페이지_번호도_검증할_수_없는_것으로_본다() {
        FindingDraft draft = draft(99, "어디에도 없는 인용문");

        assertThat(verifier.of(PAGES).accepts(draft, true)).isTrue();
        assertThat(verifier.of(PAGES).accepts(draft, false)).isFalse();
    }

    private static FindingDraft draft(int pageNo, String quote) {
        return new FindingDraft(null, Severity.ERROR, "제목", "설명", new BigDecimal("0.9"),
                pageNo, null, null,
                List.of(new EvidenceDraft(pageNo, quote, "라벨", null, null, null, null, null, null)),
                List.of());
    }
}
