package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort;
import com.logiccheck.document.port.DocumentStructurePort.BBox;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.document.port.DocumentStructurePort.PageView;
import com.logiccheck.review.finding.Severity;
import com.logiccheck.review.rule.RulesetVersionResolver;
import com.logiccheck.review.rule.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 명세 16 의 백그라운드 파이프라인 (DEV3 D-10). */
class ReviewPipelineTest {

    private static final String RULESET = "ruleset-2026.09.01";

    private ReviewPipelineStore store;
    private DocumentStructurePort structurePort;
    private RulesetVersionResolver rulesetVersionResolver;
    private ReviewPipeline pipeline;

    @BeforeEach
    void setUp() {
        store = mock(ReviewPipelineStore.class);
        structurePort = mock(DocumentStructurePort.class);
        rulesetVersionResolver = mock(RulesetVersionResolver.class);

        when(rulesetVersionResolver.currentVersion()).thenReturn(RULESET);
        when(rulesetVersionResolver.rulesOf(RULESET)).thenReturn(List.of(numericRule()));
        when(store.startRunning(42L, RULESET)).thenReturn(Optional.of(1L));
        when(structurePort.findPages(1L)).thenReturn(List.of(
                new PageView(9, 595.0, 842.0, "2027년 예상 매출 18억 원"),
                new PageView(11, 595.0, 842.0, "2027년 예상 매출 24억 원")));
        when(structurePort.findElements(1L)).thenReturn(List.of(
                element(901L, 9, "2027년 예상 매출 18억 원", "18"),
                element(902L, 11, "2027년 예상 매출 24억 원", "24")));

        pipeline = new ReviewPipeline(store, structurePort, rulesetVersionResolver,
                new QuoteVerifier(), List.of(new NumericConsistencyChecker()));
    }

    @Test
    void 착수_시_ruleset_버전을_스냅샷한다() {
        pipeline.run(42L);

        verify(store).startRunning(42L, RULESET);
    }

    @Test
    void 결정적_검산_결과를_저장하고_DONE_으로_끝낸다() {
        pipeline.run(42L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<FindingDraft>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(store).saveFindings(eq(42L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).severity()).isEqualTo(Severity.ERROR);
        verify(store).markDone(42L);
        verify(store, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void 원문과_불일치하는_초안은_저장하지_않는다() {
        when(structurePort.findPages(1L)).thenReturn(List.of(
                new PageView(9, 595.0, 842.0, "전혀 다른 본문"),
                new PageView(11, 595.0, 842.0, "전혀 다른 본문")));

        pipeline.run(42L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<FindingDraft>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(store).saveFindings(eq(42L), captor.capture());
        assertThat(captor.getValue()).isEmpty();
        verify(store).markDone(42L);
    }

    @Test
    void PENDING_이_아닌_Job_은_건너뛴다() {
        when(store.startRunning(42L, RULESET)).thenReturn(Optional.empty());

        pipeline.run(42L);

        verify(store, never()).saveFindings(anyLong(), any());
        verify(store, never()).markDone(anyLong());
        verify(store, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void 실패하면_FAILED_와_error_code_를_남긴다() {
        when(structurePort.findElements(1L)).thenThrow(new IllegalStateException("파싱 결과 조회 실패"));

        pipeline.run(42L);

        verify(store).markFailed(42L, "PIPELINE_ERROR");
        verify(store, never()).markDone(anyLong());
    }

    @Test
    void 검산기가_없는_규칙은_건너뛴다() {
        when(rulesetVersionResolver.rulesOf(RULESET)).thenReturn(List.of(ruleWithCode("UNKNOWN_RULE")));

        pipeline.run(42L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<FindingDraft>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(store).saveFindings(eq(42L), captor.capture());
        assertThat(captor.getValue()).isEmpty();
        verify(store).markDone(42L);
    }

    private static ElementView element(long id, int page, String rawText, String value) {
        return new ElementView(id, page, "NUMBER", rawText, new BigDecimal(value), "억 원",
                new BBox(0.12, 0.31, 0.66, 0.03));
    }

    private static ValidationRule numericRule() {
        return ruleWithCode(NumericConsistencyChecker.RULE_CODE);
    }

    private static ValidationRule ruleWithCode(String code) {
        ValidationRule rule = new ValidationRule() {
        };
        ReflectionTestUtils.setField(rule, "id", 7L);
        ReflectionTestUtils.setField(rule, "code", code);
        ReflectionTestUtils.setField(rule, "name", code);
        ReflectionTestUtils.setField(rule, "tolerance", new BigDecimal("0.01"));
        ReflectionTestUtils.setField(rule, "severity", Severity.ERROR);
        ReflectionTestUtils.setField(rule, "rulesetVersion", RULESET);
        ReflectionTestUtils.setField(rule, "enabled", true);
        return rule;
    }
}
