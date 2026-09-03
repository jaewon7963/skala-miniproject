package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort.BBox;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.review.pipeline.FindingDraft.Calculation;
import com.logiccheck.review.pipeline.FindingDraft.EvidenceDraft;
import com.logiccheck.review.rule.ValidationRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 같은 대상을 가리키는 수치가 문서 안에서 서로 다른 경우를 지적한다.
 *
 * 판정 방법: 요소의 rawText 에서 숫자·단위·공백·조사를 걷어낸 나머지를 "주제어" 로 보고,
 * 주제어와 단위가 같은데 numericValue 가 허용 오차를 넘게 다르면 불일치로 본다.
 *
 * 주의: 개발자2의 extracted_elements 는 주제어(label) 컬럼이 없어 rawText 정규화에 의존한다.
 * 요소 스키마에 대상 식별자가 추가되면 이 정규화를 그 값으로 교체해야 한다. work_log.md 참고.
 */
@Component
public class NumericConsistencyChecker implements DeterministicChecker {

    public static final String RULE_CODE = "NUMERIC_CONSISTENCY";

    /** 시점 표기. 이 접미사가 붙은 숫자 토큰은 값이 아니라 대상의 일부다. */
    private static final java.util.regex.Pattern TIME_SUFFIX =
            java.util.regex.Pattern.compile("(년도|년|월|일|분기|주차|주|차|회계연도|FY)");

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public List<FindingDraft> check(List<ElementView> elements, ValidationRule rule) {
        Map<String, List<ElementView>> groups = new LinkedHashMap<>();
        for (ElementView element : elements) {
            if (element.numericValue() == null || element.rawText() == null) {
                continue;
            }
            groups.computeIfAbsent(groupKey(element), key -> new ArrayList<>()).add(element);
        }

        List<FindingDraft> drafts = new ArrayList<>();
        for (Map.Entry<String, List<ElementView>> entry : groups.entrySet()) {
            List<ElementView> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            ElementView min = group.stream().min(byValue()).orElseThrow();
            ElementView max = group.stream().max(byValue()).orElseThrow();
            BigDecimal gap = max.numericValue().subtract(min.numericValue()).abs();
            if (gap.compareTo(rule.toleranceOrZero()) <= 0) {
                continue;
            }
            drafts.add(toDraft(rule, group, min, max, gap));
        }
        return drafts;
    }

    private static FindingDraft toDraft(ValidationRule rule, List<ElementView> group,
                                        ElementView min, ElementView max, BigDecimal gap) {
        String unit = max.unit() == null ? "" : max.unit();
        return new FindingDraft(
                rule.getId(),
                rule.getSeverity(),
                "같은 항목의 수치가 서로 다릅니다",
                "p.%d 의 %s 과 p.%d 의 %s 이 %s%s 만큼 어긋납니다."
                        .formatted(min.pageNo(), min.rawText(), max.pageNo(), max.rawText(),
                                gap.stripTrailingZeros().toPlainString(), unit),
                new BigDecimal("0.990"),
                min.pageNo(),
                null,
                new Calculation(
                        rule.getExpression(),
                        plain(min.numericValue()) + unit,
                        plain(max.numericValue()) + unit,
                        plain(gap) + unit),
                group.stream().map(NumericConsistencyChecker::toEvidence).toList(),
                group.stream().map(ElementView::id).toList()
        );
    }

    /** 인용문은 요소의 rawText 그대로다 — 원문에서 추출한 값이므로 재작성하지 않는다 (DEV3 D-5). */
    private static EvidenceDraft toEvidence(ElementView element) {
        BBox bbox = element.bbox();
        return new EvidenceDraft(
                element.pageNo(),
                element.rawText(),
                "p.%d · %s".formatted(element.pageNo(), element.kind()),
                bbox == null ? null : BigDecimal.valueOf(bbox.x()),
                bbox == null ? null : BigDecimal.valueOf(bbox.y()),
                bbox == null ? null : BigDecimal.valueOf(bbox.w()),
                bbox == null ? null : BigDecimal.valueOf(bbox.h()),
                null,
                null
        );
    }

    /**
     * rawText 에서 수량 토큰만 걷어내고 주제어를 남긴다.
     * "2027년 예상 매출 18억 원" 과 "2027년 예상 매출 24억 원" 은 같은 키,
     * "2026년 예상 매출 12억 원" 은 다른 키가 된다.
     *
     * 숫자를 통째로 지우면 연도까지 사라져 서로 다른 해의 수치가 한 묶음이 된다.
     * 그래서 시점 표기(년·월·일·분기·주·차)가 붙은 토큰은 주제어로 남긴다.
     */
    static String groupKey(ElementView element) {
        StringBuilder subject = new StringBuilder();
        for (String token : element.rawText().trim().split("\\s+")) {
            if (isQuantityToken(token)) {
                continue;
            }
            subject.append(token).append(' ');
        }
        return subject.toString().trim() + "|" + (element.unit() == null ? "" : element.unit());
    }

    /** 숫자로 시작하면서 시점 표기가 아닌 토큰 — 즉 값 그 자체. */
    private static boolean isQuantityToken(String token) {
        if (token.isEmpty() || !Character.isDigit(token.charAt(0))) {
            return false;
        }
        return !TIME_SUFFIX.matcher(token).find();
    }

    private static Comparator<ElementView> byValue() {
        return Comparator.comparing(ElementView::numericValue);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
