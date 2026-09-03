package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort.BBox;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.review.finding.Severity;
import com.logiccheck.review.rule.ValidationRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 결정적 검산 (DEV3 D-10 3단계). */
class NumericConsistencyCheckerTest {

    private final NumericConsistencyChecker checker = new NumericConsistencyChecker();

    @Test
    void 같은_항목의_값이_다르면_지적한다() {
        List<FindingDraft> drafts = checker.check(List.of(
                element(1L, 9, "2027년 예상 매출 18억 원", "18", "억 원"),
                element(2L, 11, "2027년 예상 매출 24억 원", "24", "억 원")
        ), rule("0.01"));

        assertThat(drafts).hasSize(1);
        FindingDraft draft = drafts.get(0);
        assertThat(draft.severity()).isEqualTo(Severity.ERROR);
        assertThat(draft.ruleId()).as("rule_id 가 있으면 method = DETERMINISTIC").isEqualTo(7L);
        assertThat(draft.calculation().expected()).isEqualTo("18억 원");
        assertThat(draft.calculation().actual()).isEqualTo("24억 원");
        assertThat(draft.calculation().diff()).isEqualTo("6억 원");
    }

    @Test
    void 지적에는_관련_요소_전부의_근거가_붙는다() {
        List<FindingDraft> drafts = checker.check(List.of(
                element(1L, 9, "2027년 예상 매출 18억 원", "18", "억 원"),
                element(2L, 11, "2027년 예상 매출 24억 원", "24", "억 원")
        ), rule("0.01"));

        FindingDraft draft = drafts.get(0);
        assertThat(draft.evidence()).hasSize(2);
        assertThat(draft.evidence()).extracting(FindingDraft.EvidenceDraft::pageNo)
                .containsExactly(9, 11);
        assertThat(draft.evidence()).extracting(FindingDraft.EvidenceDraft::quote)
                .containsExactly("2027년 예상 매출 18억 원", "2027년 예상 매출 24억 원");
        assertThat(draft.evidence().get(0).bboxX()).isEqualByComparingTo("0.12");
        assertThat(draft.elementIds()).containsExactly(1L, 2L);
    }

    @Test
    void 다른_해의_수치는_같은_항목이_아니다() {
        List<FindingDraft> drafts = checker.check(List.of(
                element(1L, 11, "2026년 예상 매출 9.6억 원", "9.6", "억 원"),
                element(2L, 11, "2027년 예상 매출 24억 원", "24", "억 원")
        ), rule("0.01"));

        assertThat(drafts).isEmpty();
    }

    @Test
    void 허용_오차_안의_차이는_지적하지_않는다() {
        List<FindingDraft> drafts = checker.check(List.of(
                element(1L, 9, "2027년 예상 매출 24억 원", "24", "억 원"),
                element(2L, 11, "2027년 예상 매출 24.005억 원", "24.005", "억 원")
        ), rule("0.01"));

        assertThat(drafts).isEmpty();
    }

    @Test
    void 단위가_다르면_같은_항목이_아니다() {
        List<FindingDraft> drafts = checker.check(List.of(
                element(1L, 9, "목표 처리량 18 건", "18", "건"),
                element(2L, 11, "목표 처리량 24 초", "24", "초")
        ), rule("0.01"));

        assertThat(drafts).isEmpty();
    }

    @Test
    void 값이_없는_요소는_건너뛴다() {
        List<FindingDraft> drafts = checker.check(List.of(
                new ElementView(1L, 9, "CLAIM", "국내 1위 기술력", null, null, null),
                new ElementView(2L, 11, "CLAIM", "국내 1위 기술력", null, null, null)
        ), rule("0.01"));

        assertThat(drafts).isEmpty();
    }

    @Test
    void 한_건만_있으면_비교_대상이_없다() {
        List<FindingDraft> drafts = checker.check(
                List.of(element(1L, 9, "2027년 예상 매출 18억 원", "18", "억 원")), rule("0.01"));

        assertThat(drafts).isEmpty();
    }

    @Test
    void bbox_가_없어도_근거는_만들어진다() {
        List<FindingDraft> drafts = checker.check(List.of(
                new ElementView(1L, 9, "NUMBER", "2027년 예상 매출 18억 원",
                        new BigDecimal("18"), "억 원", null),
                new ElementView(2L, 11, "NUMBER", "2027년 예상 매출 24억 원",
                        new BigDecimal("24"), "억 원", null)
        ), rule("0.01"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).evidence().get(0).bboxX()).isNull();
        assertThat(drafts.get(0).evidence().get(0).quote()).isNotBlank();
    }

    private static ElementView element(long id, int page, String rawText, String value, String unit) {
        return new ElementView(id, page, "NUMBER", rawText, new BigDecimal(value), unit,
                new BBox(0.12, 0.31, 0.66, 0.03));
    }

    private static ValidationRule rule(String tolerance) {
        ValidationRule rule = new ValidationRule() {
        };
        ReflectionTestUtils.setField(rule, "id", 7L);
        ReflectionTestUtils.setField(rule, "code", NumericConsistencyChecker.RULE_CODE);
        ReflectionTestUtils.setField(rule, "name", "같은 항목의 수치 일치");
        ReflectionTestUtils.setField(rule, "expression", "max(value) - min(value) <= tolerance");
        ReflectionTestUtils.setField(rule, "tolerance", new BigDecimal(tolerance));
        ReflectionTestUtils.setField(rule, "severity", Severity.ERROR);
        ReflectionTestUtils.setField(rule, "rulesetVersion", "ruleset-2026.09.01");
        ReflectionTestUtils.setField(rule, "enabled", true);
        return rule;
    }
}
