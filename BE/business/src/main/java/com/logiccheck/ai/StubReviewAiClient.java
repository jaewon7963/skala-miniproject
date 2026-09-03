package com.logiccheck.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.logiccheck.ai.ReviewAiRequest.AnalyzedBlock;
import com.logiccheck.ai.ReviewAiRequest.AnalyzedPage;
import com.logiccheck.ai.ReviewAiResult.AiCalculation;
import com.logiccheck.ai.ReviewAiResult.AiEvidence;
import com.logiccheck.ai.ReviewAiResult.AiFinding;

/**
 * 모델 없이 규칙만으로 검토 항목을 만들어 내는 구현체.
 *
 * <p>여기서 나오는 항목은 전부 문서에 실제로 적힌 내용에 붙는다. 근거의 앵커는 항상
 * 원문 블록 id라서, 화면에서 항목을 누르면 진짜 그 문장으로 이동한다.
 * 같은 문서를 다시 분석하면 같은 결과가 나오도록 확신도까지 블록 id에서 파생시킨다.
 *
 * <p>실제 모델을 붙일 때는 이 클래스를 대체하는 구현을 추가하고 {@code review.ai.provider}
 * 값만 바꾸면 된다. 저장 쪽은 인터페이스만 알고 있다.
 */
@Component
@ConditionalOnProperty(name = "review.ai.provider", havingValue = "stub", matchIfMissing = true)
public class StubReviewAiClient implements ReviewAiClient {

    private static final String MODEL_VERSION = "rule-based-0.1";
    private static final String PROMPT_VERSION = "review-v1";

    private static final Pattern TOTAL_LABEL = Pattern.compile("^(합계|계|총계|소계|Total|TOTAL)$");
    private static final Pattern GROWTH_RATE = Pattern.compile("(CAGR|연평균\\s*성장률|성장률)");
    private static final Pattern MARKET_SIZE = Pattern.compile("(시장\\s*규모|시장은|전망|추정된다|성장할\\s*것)");
    private static final Pattern SOURCE_MARKER = Pattern.compile("(출처|자료|근거|기준|조사|보고서|참조|인용)");
    private static final Pattern KPI_LABEL = Pattern.compile("(KPI|지표|목표|달성률|정확도|재방문)");
    private static final Pattern EMPTY_CELL = Pattern.compile("^[\\s\\-—]*$");

    private static final int MIN_FINDINGS = 3;
    private static final int MAX_FINDINGS = 12;
    /** 표 합계는 자릿수 반올림 차이가 있어 1% 안쪽 오차는 불일치로 보지 않는다. */
    private static final double TOLERANCE_RATIO = 0.01;

    @Override
    public ReviewAiResult analyze(ReviewAiRequest request) {
        List<AiFinding> findings = new ArrayList<>();
        findings.addAll(tableTotalMismatches(request));
        findings.addAll(conflictingAssumptions(request));
        findings.addAll(unsourcedClaims(request));
        findings.addAll(unmeasurableTargets(request));
        if (findings.size() < MIN_FINDINGS) {
            findings.addAll(reviewWorthyParagraphs(request, MIN_FINDINGS - findings.size(), findings));
        }

        List<AiFinding> kept = findings.stream()
                .filter(f -> f.evidence() != null && !f.evidence().isEmpty())
                .sorted(Comparator.comparingInt(AiFinding::page)
                        .thenComparing(Comparator.comparingDouble(AiFinding::confidence).reversed()))
                .limit(MAX_FINDINGS)
                .toList();

        return new ReviewAiResult(MODEL_VERSION, PROMPT_VERSION, kept);
    }

    @Override
    public ReviewAiAnswer ask(ReviewAiQuestion question) {
        if (question.question() == null || question.question().isBlank()) {
            return ReviewAiAnswer.notFound();
        }
        List<String> keywords = keywordsOf(question.question());
        if (keywords.isEmpty()) {
            return ReviewAiAnswer.notFound();
        }

        return question.knownFindings().stream()
                .map(finding -> Map.entry(finding, score(finding, keywords)))
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(entry -> answerFrom(entry.getKey(), question.selection()))
                .orElseGet(ReviewAiAnswer::notFound);
    }

    /* ------------------------------------------------------------------ */
    /* 규칙 1 — 표의 합계 행이 실제 합과 맞는지 다시 계산한다                  */
    /* ------------------------------------------------------------------ */

    private List<AiFinding> tableTotalMismatches(ReviewAiRequest request) {
        List<AiFinding> findings = new ArrayList<>();
        for (AnalyzedPage page : request.pages()) {
            for (AnalyzedBlock block : page.blocks()) {
                if (!block.isTable() || block.rows() == null || block.rows().size() < 2) {
                    continue;
                }
                totalRowMismatch(page, block).ifPresent(findings::add);
            }
        }
        return findings;
    }

    private Optional<AiFinding> totalRowMismatch(AnalyzedPage page, AnalyzedBlock table) {
        List<List<String>> rows = table.rows();
        int totalRowIndex = indexOfTotalRow(rows);
        if (totalRowIndex < 0) {
            return Optional.empty();
        }

        List<String> totalRow = rows.get(totalRowIndex);
        for (int column = 1; column < totalRow.size(); column++) {
            Optional<Double> stated = NumberText.parse(cell(totalRow, column));
            if (stated.isEmpty()) {
                continue;
            }
            Double computed = sumColumn(rows, column, totalRowIndex);
            if (computed == null || matches(stated.get(), computed)) {
                continue;
            }
            return Optional.of(mismatchFinding(page, table, column, stated.get(), computed, rows, totalRowIndex));
        }
        return Optional.empty();
    }

    private AiFinding mismatchFinding(AnalyzedPage page, AnalyzedBlock table, int column, double stated,
                                       double computed, List<List<String>> rows, int totalRowIndex) {
        String label = columnLabel(table, column);
        String expression = rows.stream()
                .filter(row -> rows.indexOf(row) != totalRowIndex)
                .map(row -> cell(row, column))
                .filter(value -> NumberText.parse(value).isPresent())
                .reduce((left, right) -> left + " + " + right)
                .orElse("");

        String caption = table.caption() == null ? "표" : table.caption();
        return new AiFinding(
                "ERROR",
                "DETERMINISTIC",
                page.sectionId(),
                page.pageNo(),
                caption + " · " + label + " 값이 항목 합과 맞지 않습니다",
                "표에 적힌 합계는 " + NumberText.format(stated) + "인데 각 항목을 더하면 "
                        + NumberText.format(computed) + "입니다 · 차이 " + NumberText.format(Math.abs(stated - computed)),
                confidence(0.95, table.id()),
                new AiCalculation(expression, NumberText.format(computed), NumberText.format(stated),
                        NumberText.format(Math.abs(stated - computed))),
                List.of(new AiEvidence(table.id(), page.pageNo(), caption + " 합계 행")));
    }

    private int indexOfTotalRow(List<List<String>> rows) {
        for (int i = 0; i < rows.size(); i++) {
            String first = cell(rows.get(i), 0).trim();
            if (TOTAL_LABEL.matcher(first).matches()) {
                return i;
            }
        }
        return -1;
    }

    private Double sumColumn(List<List<String>> rows, int column, int skipRow) {
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (i == skipRow) {
                continue;
            }
            Optional<Double> value = NumberText.parse(cell(rows.get(i), column));
            if (value.isPresent()) {
                sum += value.get();
                counted++;
            }
        }
        return counted >= 2 ? sum : null;
    }

    private boolean matches(double stated, double computed) {
        double scale = Math.max(Math.abs(stated), Math.abs(computed));
        return Math.abs(stated - computed) <= scale * TOLERANCE_RATIO;
    }

    /* ------------------------------------------------------------------ */
    /* 규칙 2 — 같은 대상을 세는 숫자가 페이지마다 다른 경우                   */
    /* ------------------------------------------------------------------ */

    private List<AiFinding> conflictingAssumptions(ReviewAiRequest request) {
        Map<String, List<Mention>> byNoun = new LinkedHashMap<>();
        for (AnalyzedPage page : request.pages()) {
            for (AnalyzedBlock block : page.blocks()) {
                String text = plainText(block);
                for (NumberText.Count count : NumberText.counts(text)) {
                    byNoun.computeIfAbsent(count.noun(), key -> new ArrayList<>())
                            .add(new Mention(page, block, count.value()));
                }
            }
        }

        List<AiFinding> findings = new ArrayList<>();
        for (Map.Entry<String, List<Mention>> entry : byNoun.entrySet()) {
            List<Mention> mentions = entry.getValue();
            if (mentions.size() < 2) {
                continue;
            }
            Mention low = mentions.stream().min(Comparator.comparingDouble(Mention::value)).orElseThrow();
            Mention high = mentions.stream().max(Comparator.comparingDouble(Mention::value)).orElseThrow();
            if (low.value() == high.value() || low.block().id().equals(high.block().id())) {
                continue;
            }
            findings.add(new AiFinding(
                    "ERROR",
                    "DETERMINISTIC",
                    high.page().sectionId(),
                    high.page().pageNo(),
                    entry.getKey() + " 수 전제가 서로 다릅니다",
                    "p." + low.page().pageNo() + "은 " + (long) low.value() + "개, p." + high.page().pageNo()
                            + "은 " + (long) high.value() + "개를 전제로 합니다. 어느 쪽이 맞는지 확인이 필요합니다.",
                    confidence(0.88, high.block().id()),
                    null,
                    List.of(new AiEvidence(low.block().id(), low.page().pageNo(),
                                    (long) low.value() + "개 " + entry.getKey()),
                            new AiEvidence(high.block().id(), high.page().pageNo(),
                                    (long) high.value() + "개 " + entry.getKey()))));
        }
        return findings;
    }

    /* ------------------------------------------------------------------ */
    /* 규칙 3 — 수치를 단정했는데 출처가 없는 문장                            */
    /* ------------------------------------------------------------------ */

    private List<AiFinding> unsourcedClaims(ReviewAiRequest request) {
        List<AiFinding> findings = new ArrayList<>();
        for (AnalyzedPage page : request.pages()) {
            for (AnalyzedBlock block : page.blocks()) {
                if (block.isTable() || block.text() == null) {
                    continue;
                }
                String text = block.text();
                if (SOURCE_MARKER.matcher(text).find()) {
                    continue;
                }
                if (GROWTH_RATE.matcher(text).find() && text.contains("%")) {
                    findings.add(claim(page, block, "NEEDS_CHECK", 0.78,
                            "성장률의 산출 근거를 찾지 못했습니다",
                            "문서 안에서 이 성장률을 뒷받침하는 자료나 계산 과정이 확인되지 않습니다."));
                } else if (MARKET_SIZE.matcher(text).find() && NumberText.parse(text).isPresent()) {
                    findings.add(claim(page, block, "NO_EVIDENCE", 0.7,
                            "시장 전망 수치의 출처가 없습니다",
                            "인용한 조사 기관이나 산출 방식이 기재되어 있지 않습니다."));
                }
            }
        }
        return findings;
    }

    /* ------------------------------------------------------------------ */
    /* 규칙 4 — 목표는 있는데 재는 방법이 비어 있는 지표                      */
    /* ------------------------------------------------------------------ */

    private List<AiFinding> unmeasurableTargets(ReviewAiRequest request) {
        List<AiFinding> findings = new ArrayList<>();
        for (AnalyzedPage page : request.pages()) {
            for (AnalyzedBlock block : page.blocks()) {
                if (!block.isTable() || block.rows() == null) {
                    continue;
                }
                String header = String.join(" ", block.head() == null ? List.of() : block.head());
                String caption = block.caption() == null ? "" : block.caption();
                if (!KPI_LABEL.matcher(header + " " + caption).find()) {
                    continue;
                }
                boolean hasBlank = block.rows().stream()
                        .anyMatch(row -> row.stream().skip(1).anyMatch(cell -> EMPTY_CELL.matcher(cell).matches()));
                if (hasBlank) {
                    findings.add(new AiFinding("NO_EVIDENCE", "RAG", page.sectionId(), page.pageNo(),
                            "지표의 측정 방법이 비어 있습니다",
                            "목표치는 제시했지만 측정 주기·산식·데이터 출처 중 채워지지 않은 칸이 있습니다.",
                            confidence(0.72, block.id()), null,
                            List.of(new AiEvidence(block.id(), page.pageNo(),
                                    caption.isBlank() ? "지표 표" : caption))));
                }
            }
        }
        return findings;
    }

    /* ------------------------------------------------------------------ */
    /* 보완 — 위 규칙에 걸린 게 적으면 분량이 큰 문단을 확인 대상으로 올린다   */
    /* ------------------------------------------------------------------ */

    private List<AiFinding> reviewWorthyParagraphs(ReviewAiRequest request, int wanted, List<AiFinding> existing) {
        List<String> taken = existing.stream()
                .flatMap(f -> f.evidence().stream())
                .map(AiEvidence::anchorId)
                .toList();

        return request.pages().stream()
                .flatMap(page -> page.blocks().stream().map(block -> new Mention(page, block, 0)))
                .filter(m -> "p".equals(m.block().kind()))
                .filter(m -> m.block().text() != null && m.block().text().length() >= 40)
                .filter(m -> !taken.contains(m.block().id()))
                .sorted(Comparator.comparingInt((Mention m) -> m.block().text().length()).reversed())
                .limit(wanted)
                .map(m -> claim(m.page(), m.block(), "NEEDS_CHECK", 0.61,
                        "핵심 서술이라 사람 확인이 필요합니다",
                        "분량과 위치로 볼 때 사업 판단의 근거가 되는 문단입니다. 수치와 전제가 뒤 내용과 맞는지 확인해주세요."))
                .toList();
    }

    private AiFinding claim(AnalyzedPage page, AnalyzedBlock block, String type, double baseConfidence, String title,
                             String description) {
        return new AiFinding(type, "RAG", page.sectionId(), page.pageNo(), title, description,
                confidence(baseConfidence, block.id()), null,
                List.of(new AiEvidence(block.id(), page.pageNo(), excerpt(block.text()))));
    }

    /* ------------------------------------------------------------------ */
    /* 질의응답                                                            */
    /* ------------------------------------------------------------------ */

    private List<String> keywordsOf(String question) {
        return List.of(question.toLowerCase(Locale.KOREAN).split("[\\s,.?!·]+")).stream()
                .filter(word -> word.length() >= 2)
                .toList();
    }

    private int score(ReviewAiQuestion.KnownFinding finding, List<String> keywords) {
        String haystack = (finding.title() + " " + finding.description()).toLowerCase(Locale.KOREAN);
        return (int) keywords.stream().filter(haystack::contains).count();
    }

    private ReviewAiAnswer answerFrom(ReviewAiQuestion.KnownFinding finding, String selection) {
        String where = selection == null || selection.isBlank() ? "문서 전체" : "선택한 구절";
        String answer = where + "를 기준으로 보면, p." + finding.page() + "의 \"" + finding.title() + "\" 건이 관련됩니다. "
                + finding.description();
        AiFinding draft = new AiFinding(finding.type(), "RAG", null, finding.page(), finding.title(),
                finding.description(), 0.8, null, finding.evidence());
        return new ReviewAiAnswer(answer, finding.evidence(), true, draft);
    }

    /* ------------------------------------------------------------------ */

    /**
     * 같은 문서를 다시 분석해도 확신도가 흔들리지 않도록 블록 id에서 값을 만든다.
     * 규칙마다 기준값을 두고 ±0.04 범위에서만 흔든다.
     */
    private double confidence(double base, String anchorId) {
        int spread = Math.floorMod(anchorId.hashCode(), 9) - 4;
        double value = base + spread * 0.01;
        return Math.round(Math.max(0.5, Math.min(0.99, value)) * 100) / 100.0;
    }

    private String plainText(AnalyzedBlock block) {
        if (!block.isTable()) {
            return block.text() == null ? "" : block.text();
        }
        StringBuilder builder = new StringBuilder();
        if (block.caption() != null) {
            builder.append(block.caption()).append(' ');
        }
        if (block.rows() != null) {
            block.rows().forEach(row -> builder.append(String.join(" ", row)).append(' '));
        }
        return builder.toString();
    }

    private String columnLabel(AnalyzedBlock table, int column) {
        List<String> head = table.head();
        if (head != null && column < head.size() && !head.get(column).isBlank()) {
            return head.get(column);
        }
        return (column + 1) + "번째 열";
    }

    private String cell(List<String> row, int column) {
        return column < row.size() ? row.get(column) : "";
    }

    private String excerpt(String text) {
        if (text == null) {
            return "본문 문단";
        }
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }

    private record Mention(AnalyzedPage page, AnalyzedBlock block, double value) {
    }
}
