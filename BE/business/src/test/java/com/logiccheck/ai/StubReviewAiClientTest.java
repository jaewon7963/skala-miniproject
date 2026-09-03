package com.logiccheck.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.logiccheck.ai.ReviewAiRequest.AnalyzedBlock;
import com.logiccheck.ai.ReviewAiRequest.AnalyzedPage;
import com.logiccheck.ai.ReviewAiResult.AiEvidence;
import com.logiccheck.ai.ReviewAiResult.AiFinding;

class StubReviewAiClientTest {

    private final StubReviewAiClient client = new StubReviewAiClient();

    @Test
    void findsTableTotalThatDoesNotAddUp() {
        AnalyzedBlock table = new AnalyzedBlock("b-11-3", "table", null, "[표 5-1] 연도별 매출 추정",
                List.of("구분", "2025", "2026", "합계"),
                List.of(List.of("매출", "3.2억", "9.6억", "12.8억"),
                        List.of("원가", "2.1억", "5.8억", "7.9억"),
                        List.of("합계", "5.3억", "15.4억", "30억")));

        ReviewAiResult result = client.analyze(request(page(11, table)));

        AiFinding mismatch = result.findings().stream()
                .filter(f -> f.method().equals("DETERMINISTIC"))
                .findFirst()
                .orElseThrow();
        assertThat(mismatch.type()).isEqualTo("ERROR");
        // 표시용 문자열이 아니라 숫자로 넘긴다 — 저장하는 쪽이 숫자 컬럼이다.
        assertThat(mismatch.calculation().actual()).isEqualTo(30_0000_0000d);
        assertThat(mismatch.calculation().expected()).isEqualTo(20_7000_0000d);
        assertThat(mismatch.evidence()).extracting(AiEvidence::anchorId).containsExactly("b-11-3");
    }

    @Test
    void findsTheSameThingCountedDifferentlyOnTwoPages() {
        ReviewAiResult result = client.analyze(new ReviewAiRequest("사업계획서", List.of(
                page(9, new AnalyzedBlock("b-9-2", "p", "2027년까지 3개 지역에 40개 매장을 확보한다.", null, null, null)),
                page(11, new AnalyzedBlock("b-11-4", "p", "2027년 매출은 62개 매장 기준으로 산출하였다.", null, null, null)))));

        AiFinding conflict = result.findings().stream()
                .filter(f -> f.title().contains("매장"))
                .findFirst()
                .orElseThrow();
        assertThat(conflict.type()).isEqualTo("ERROR");
        assertThat(conflict.evidence()).extracting(AiEvidence::anchorId).containsExactly("b-9-2", "b-11-4");
    }

    @Test
    void neverReturnsAFindingWithoutEvidence() {
        ReviewAiResult result = client.analyze(request(page(1,
                new AnalyzedBlock("b-1-1", "h2", "1. 사업 개요", null, null, null))));

        assertThat(result.findings()).allSatisfy(finding -> assertThat(finding.evidence()).isNotEmpty());
    }

    @Test
    void producesTheSameResultForTheSameDocument() {
        ReviewAiRequest request = request(page(4,
                new AnalyzedBlock("b-4-2", "p", "2027년 국내 시장은 1.2조 원 규모로 성장할 것으로 전망된다.", null, null, null)));

        ReviewAiResult first = client.analyze(request);
        ReviewAiResult second = client.analyze(request);

        assertThat(first.findings()).isEqualTo(second.findings());
    }

    @Test
    void saysItCannotAnswerWhenNothingMatches() {
        ReviewAiAnswer answer = client.ask(new ReviewAiQuestion("전혀 관계없는 질문", null, List.of(), List.of()));

        assertThat(answer.answer()).isEqualTo("이 문서 안에서 관련 근거를 찾지 못했습니다. 추측하지 않습니다.");
        assertThat(answer.evidences()).isEmpty();
        assertThat(answer.promotable()).isFalse();
    }

    @Test
    void answersFromAnExistingFindingWhenTheQuestionMatches() {
        ReviewAiQuestion.KnownFinding known = new ReviewAiQuestion.KnownFinding("ERROR", 11,
                "매출 합계가 표와 맞지 않습니다", "본문과 표 5-1의 값이 다릅니다",
                List.of(new AiEvidence("b-11-3", 11, "표 5-1 합계 행")));

        ReviewAiAnswer answer = client.ask(new ReviewAiQuestion("매출 합계가 맞는지 봐줘", null, List.of(), List.of(known)));

        assertThat(answer.promotable()).isTrue();
        assertThat(answer.evidences()).extracting(AiEvidence::anchorId).containsExactly("b-11-3");
        assertThat(answer.findingDraft().title()).isEqualTo(known.title());
    }

    private ReviewAiRequest request(AnalyzedPage... pages) {
        return new ReviewAiRequest("사업계획서", List.of(pages));
    }

    private AnalyzedPage page(int pageNo, AnalyzedBlock... blocks) {
        return new AnalyzedPage(pageNo, null, null, List.of(blocks));
    }
}
