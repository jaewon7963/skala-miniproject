package com.logiccheck.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.parse.PageBlockExtractor;

class PageBlockExtractorTest {

    private final PageBlockExtractor extractor = new PageBlockExtractor();

    @Test
    void splitsHeadingsParagraphsAndTables() {
        String text = """
                5. 매출 · 재무 계획
                2027년 매출 24억 원, 영업이익률 18%를 목표로 한다.
                [표 5-1] 연도별 매출 추정
                구분      2025      2026      합계
                매출      3.2억     9.6억     12.8억
                원가      2.1억     5.8억     7.9억
                """;

        List<PageBlock> blocks = extractor.extract(11, text);

        assertThat(blocks).extracting(PageBlock::kind).containsExactly("h2", "p", "table");
        assertThat(blocks).extracting(PageBlock::id).containsExactly("b-11-1", "b-11-2", "b-11-3");

        PageBlock table = blocks.get(2);
        assertThat(table.caption()).isEqualTo("[표 5-1] 연도별 매출 추정");
        assertThat(table.head()).containsExactly("구분", "2025", "2026", "합계");
        assertThat(table.rows()).hasSize(2);
        assertThat(table.rows().get(0)).containsExactly("매출", "3.2억", "9.6억", "12.8억");
    }

    @Test
    void marksPagesWithoutTextLayerAsFigure() {
        List<PageBlock> blocks = extractor.extract(14, "   \n\n  ");

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).kind()).isEqualTo("figure");
        assertThat(blocks.get(0).id()).isEqualTo("b-14-1");
    }

    @Test
    void joinsWrappedLinesIntoOneParagraph() {
        List<PageBlock> blocks = extractor.extract(3, """
                초기 목표 시장은 수도권 대형 프랜차이즈 매장으로 설정한다.
                2026년까지 시범 운영을 마치고 본격적인 확장에 들어간다.
                """);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).kind()).isEqualTo("p");
        assertThat(blocks.get(0).text()).contains("수도권").contains("시범 운영");
    }
}
