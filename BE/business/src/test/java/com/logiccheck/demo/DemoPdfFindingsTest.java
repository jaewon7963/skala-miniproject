package com.logiccheck.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.logiccheck.ai.ReviewAiRequest;
import com.logiccheck.ai.ReviewAiResult;
import com.logiccheck.ai.StubReviewAiClient;
import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.parse.PageBlockExtractor;
import com.logiccheck.document.parse.ParsedDocument;
import com.logiccheck.document.parse.PdfStructureExtractor;

/**
 * 커밋된 시연용 PDF가 여전히 의도한 검토 항목을 만들어 내는지 지킨다.
 *
 * <p>시연 화면은 이 다섯 건 위에서 돌아간다. 파서나 규칙을 손대면 PDF는 그대로인데
 * 결과만 조용히 달라질 수 있어서, 그 순간 여기서 먼저 깨지도록 해 둔다.
 * 스프링 컨텍스트도 DB도 필요 없다.
 */
class DemoPdfFindingsTest {

    private static final String RESOURCE = "/demo/bizxray-demo-plan.pdf";

    private static ParsedDocument parsed;
    private static Map<Integer, List<PageBlock>> blocksByPage;
    private static ReviewAiResult result;

    @BeforeAll
    static void analyze() throws IOException {
        Path file = Files.createTempFile("bizxray-demo", ".pdf");
        try (InputStream in = DemoPdfFindingsTest.class.getResourceAsStream(RESOURCE)) {
            assertThat(in).as("데모 PDF 리소스 %s", RESOURCE).isNotNull();
            Files.copy(in, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        parsed = new PdfStructureExtractor().extract(file);
        PageBlockExtractor extractor = new PageBlockExtractor();
        blocksByPage = parsed.pages().stream()
                .collect(Collectors.toMap(ParsedDocument.ParsedPage::pageNo,
                        page -> extractor.extract(page.pageNo(), page.text())));

        List<ReviewAiRequest.AnalyzedPage> pages = parsed.pages().stream()
                .map(page -> new ReviewAiRequest.AnalyzedPage(page.pageNo(), null, null,
                        blocksByPage.get(page.pageNo()).stream()
                                .map(block -> new ReviewAiRequest.AnalyzedBlock(block.id(), block.kind(),
                                        block.text(), block.caption(),
                                        block.head() == null ? List.of() : block.head(),
                                        block.rows() == null ? List.of() : block.rows()))
                                .toList()))
                .toList();

        result = new StubReviewAiClient().analyze(new ReviewAiRequest(DemoPdfContent.TITLE, pages));
        Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("8페이지, 목차 8개(대제목 6 + 소제목 2)로 파싱된다")
    void structure() {
        assertThat(parsed.pageCount()).isEqualTo(8);
        assertThat(parsed.sections()).extracting(ParsedDocument.ParsedSection::title)
                .containsExactly("1. 사업 개요", "2. 시장 분석", "2.1 참고 자료", "3. 성장 시나리오", "3.1 경쟁 구도",
                        "4. 매출 계획", "5. 확대 계획", "6. 핵심 성과지표");
    }

    @Test
    @DisplayName("표 두 개가 표 블록으로 읽힌다 — 캡션과 머리행까지")
    void tablesAreParsedAsTables() {
        PageBlock revenue = tableOn(6);
        assertThat(revenue.caption()).isEqualTo("[표 1] 연도별 매출 계획 (단위: 백만원)");
        assertThat(revenue.head()).containsExactly("구분", "2026년", "2027년", "2028년");
        assertThat(revenue.rows()).containsExactly(
                List.of("제품 매출", "1,200", "2,400", "4,800"),
                List.of("서비스 매출", "300", "700", "1,500"),
                List.of("합계", "1,500", "3,100", "7,000"));

        PageBlock kpi = tableOn(8);
        assertThat(kpi.caption()).isEqualTo("[표 2] 핵심 성과지표(KPI) 목표");
        assertThat(kpi.head()).containsExactly("지표", "2026년 목표", "측정 주기", "산식");
        assertThat(kpi.rows()).allSatisfy(row -> assertThat(row).hasSize(4));
    }

    @Test
    @DisplayName("검토 항목 5건이 규칙별로 하나씩 나온다")
    void findings() {
        assertThat(result.findings()).hasSize(5);

        assertThat(result.findings()).extracting(ReviewAiResult.AiFinding::type)
                .containsExactlyInAnyOrder("NO_EVIDENCE", "NEEDS_CHECK", "ERROR", "ERROR", "NO_EVIDENCE");

        assertThat(finding(2).title()).isEqualTo("시장 전망 수치의 출처가 없습니다");
        assertThat(finding(4).title()).isEqualTo("성장률의 산출 근거를 찾지 못했습니다");
        assertThat(finding(7).title()).isEqualTo("매장 수 전제가 서로 다릅니다");
        assertThat(finding(8).title()).isEqualTo("지표의 측정 방법이 비어 있습니다");
    }

    @Test
    @DisplayName("표 합계 오류는 틀린 열 하나만 짚고 계산 과정을 남긴다")
    void tableTotalMismatch() {
        ReviewAiResult.AiFinding mismatch = finding(6);

        assertThat(mismatch.type()).isEqualTo("ERROR");
        assertThat(mismatch.method()).isEqualTo("DETERMINISTIC");
        assertThat(mismatch.title()).isEqualTo("[표 1] 연도별 매출 계획 (단위: 백만원) · 2028년 값이 항목 합과 맞지 않습니다");
        assertThat(mismatch.calculation()).isNotNull();
        assertThat(mismatch.calculation().expression()).isEqualTo("4,800 + 1,500");
        assertThat(mismatch.calculation().expected()).isEqualTo("6300");
        assertThat(mismatch.calculation().actual()).isEqualTo("7000");
        assertThat(mismatch.calculation().diff()).isEqualTo("700");
    }

    @Test
    @DisplayName("출처를 밝힌 문단은 지적하지 않는다")
    void sourcedParagraphIsNotFlagged() {
        List<String> flagged = result.findings().stream()
                .flatMap(f -> f.evidence().stream())
                .map(ReviewAiResult.AiEvidence::anchorId)
                .toList();

        String sourced = blocksByPage.get(3).stream()
                .filter(block -> block.text() != null && block.text().contains("한국로봇산업진흥원"))
                .map(PageBlock::id)
                .findFirst()
                .orElseThrow();

        assertThat(flagged).doesNotContain(sourced);
    }

    @Test
    @DisplayName("모든 근거 앵커가 실제 원문 블록을 가리킨다 — 화면의 하이라이트 점프가 사는 조건")
    void everyAnchorPointsAtARealBlock() {
        for (ReviewAiResult.AiFinding f : result.findings()) {
            assertThat(f.evidence()).isNotEmpty();
            for (ReviewAiResult.AiEvidence evidence : f.evidence()) {
                List<String> ids = blocksByPage.get(evidence.page()).stream().map(PageBlock::id).toList();
                assertThat(ids)
                        .as("p.%d 의 앵커 %s", evidence.page(), evidence.anchorId())
                        .contains(evidence.anchorId());
            }
        }
    }

    private static ReviewAiResult.AiFinding finding(int page) {
        return result.findings().stream()
                .filter(f -> f.page() == page)
                .findFirst()
                .orElseThrow(() -> new AssertionError("p." + page + " 에 검토 항목이 없습니다"));
    }

    private static PageBlock tableOn(int pageNo) {
        return blocksByPage.get(pageNo).stream()
                .filter(PageBlock::isTable)
                .findFirst()
                .orElseThrow(() -> new AssertionError("p." + pageNo + " 에 표 블록이 없습니다"));
    }
}
