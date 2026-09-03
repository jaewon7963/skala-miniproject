package com.logiccheck.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.parse.DocumentOutline;
import com.logiccheck.document.parse.PageBlockExtractor;
import com.logiccheck.document.parse.ParsedDocument;

/**
 * 목차와 요약이 실제로 읽을 만하게 나오는지 고정한다.
 *
 * <p>여기 있는 사례는 전부 실제 문서에서 났던 일이다 — 발표자료 한 건이 373개짜리 목차를
 * 만들었고, 보고서 한 건은 쪽마다 찍히는 머리글로 목차를 채웠으며, 또 한 건은 표지의
 * 제출일이 요약으로 올라갔다.
 */
class DocumentOutlineTest {

    private final PageBlockExtractor extractor = new PageBlockExtractor();

    @Test
    void keepsTheOutlineShortEvenWhenTheBookmarkTreeIsHuge() {
        List<ParsedDocument.ParsedSection> huge = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            huge.add(new ParsedDocument.ParsedSection("항목 " + i, 1, 1, i));
        }

        DocumentOutline.Result result = DocumentOutline.build(huge, pages("본문입니다."));

        assertThat(result.entries()).hasSize(40);
    }

    @Test
    void collapsesTheRunningHeaderThatRepeatsOnEveryPage() {
        // 쪽마다 같은 머리글이 반복되는 문서. 제목 블록으로 잡혀 목차가 머리글로 도배된다.
        Map<Integer, List<PageBlock>> blocks = new LinkedHashMap<>();
        for (int pageNo = 1; pageNo <= 8; pageNo++) {
            blocks.put(pageNo, extractor.extract(pageNo, "서브 노트 1 SKALA\n본문 내용이 이어집니다."));
        }

        DocumentOutline.Result result = DocumentOutline.build(List.of(), blocks);

        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().get(0).title()).isEqualTo("서브 노트 1 SKALA");
    }

    @Test
    void keepsTheSameTitleWhenItComesBackAfterSomethingElse() {
        // 붙어 있지 않은 반복은 머리글이 아니라 실제로 다시 나온 절이다. 합치면 안 된다.
        List<ParsedDocument.ParsedSection> bookmarks = List.of(
                new ParsedDocument.ParsedSection("개요", 1, 1, 0),
                new ParsedDocument.ParsedSection("본론", 1, 2, 1),
                new ParsedDocument.ParsedSection("개요", 1, 3, 2));

        DocumentOutline.Result result = DocumentOutline.build(bookmarks, pages("본문입니다."));

        assertThat(result.entries()).extracting(DocumentOutline.Entry::title)
                .containsExactly("개요", "본론", "개요");
    }

    @Test
    void marksWhereTheOutlineCameFrom() {
        DocumentOutline.Result fromBookmarks = DocumentOutline.build(
                List.of(new ParsedDocument.ParsedSection("1. 개요", 1, 1, 0)), pages("본문입니다."));
        DocumentOutline.Result guessed = DocumentOutline.build(List.of(), pages("1. 개요\n본문입니다."));

        assertThat(fromBookmarks.source()).isEqualTo(Section.Source.ORIGINAL);
        assertThat(guessed.source()).isEqualTo(Section.Source.EXTRACTED);
        assertThat(guessed.entries()).isNotEmpty();
    }

    @Test
    void linksChildEntriesToTheNearestShallowerParent() {
        DocumentOutline.Result result = DocumentOutline.build(List.of(
                new ParsedDocument.ParsedSection("1. 사업 개요", 1, 1, 0),
                new ParsedDocument.ParsedSection("1.1 배경", 2, 1, 1),
                new ParsedDocument.ParsedSection("1.2 목표", 2, 2, 2),
                new ParsedDocument.ParsedSection("2. 시장 분석", 1, 3, 3)), pages("본문입니다."));

        assertThat(result.entries()).extracting(DocumentOutline.Entry::parentIndex)
                .containsExactly(-1, 0, 0, -1);
    }

    @Test
    void dropsBlankTitles() {
        DocumentOutline.Result result = DocumentOutline.build(List.of(
                new ParsedDocument.ParsedSection("  ", 1, 1, 0),
                new ParsedDocument.ParsedSection("1. 개요", 1, 1, 1)), pages("본문입니다."));

        assertThat(result.entries()).extracting(DocumentOutline.Entry::title).containsExactly("1. 개요");
        assertThat(result.entries().get(0).orderNo()).isZero();
    }

    @Test
    void summarisesOnlyWithSomethingThatReadsLikeASentence() {
        // 목차 네비게이션 라벨은 문장이 아니다.
        assertThat(DocumentOutline.summarize(pages("Problem Insight Solution Design Demo Next"))).isNull();
        // 표지의 제출일 칸도 아니다.
        assertThat(DocumentOutline.summarize(pages("제  출  일 2026. 08. 13."))).isNull();

        assertThat(DocumentOutline.summarize(pages("AI 매장 안내 로봇을 개발해 무인 매장에 공급하는 사업이다.")))
                .isEqualTo("AI 매장 안내 로봇을 개발해 무인 매장에 공급하는 사업이다.");
    }

    private Map<Integer, List<PageBlock>> pages(String... texts) {
        Map<Integer, List<PageBlock>> blocks = new LinkedHashMap<>();
        for (int i = 0; i < texts.length; i++) {
            blocks.put(i + 1, extractor.extract(i + 1, texts[i]));
        }
        return blocks;
    }
}
