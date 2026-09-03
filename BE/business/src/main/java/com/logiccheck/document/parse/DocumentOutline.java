package com.logiccheck.document.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.entity.Section;

/**
 * 파싱 결과에서 목차와 한 줄 요약을 만든다.
 *
 * <p>저장과 분리해 둔 이유는 여기 규칙이 문서마다 제일 자주 어긋나는 부분이기 때문이다.
 * DB 없이 문서 모양만 바꿔가며 검증할 수 있어야 한다.
 */
public final class DocumentOutline {

    /** "2.1 목표 시장 규모"처럼 소수점이 있으면 하위 목차로 본다. */
    private static final Pattern SUB_HEADING = Pattern.compile("^\\d+\\.\\d+.*");
    /** 한국어 문장이 흔히 끝나는 어미. 마침표 없이 끝나는 문단을 살리기 위한 것. */
    private static final Pattern KOREAN_ENDING = Pattern.compile(".*(다|요|음|임)$");

    /**
     * 목차 상한. 북마크에서 왔든 본문에서 추정했든 같은 상한을 적용한다.
     * 예전에는 추정 경로에만 상한이 있어서, 북마크가 있는 발표자료가 373개짜리 목차를 만들었다.
     */
    static final int MAX_SECTIONS = 40;
    private static final int SUMMARY_MAX_LENGTH = 120;
    private static final int SUMMARY_MIN_LENGTH = 20;

    private DocumentOutline() {
    }

    /**
     * 목차 항목 하나. {@code parentIndex} 는 같은 목록 안에서의 부모 위치이며 최상위는 -1이다.
     * 저장하는 쪽은 이 값만 보고 계층을 잇는다.
     */
    public record Entry(String title, int level, Integer pageFrom, int orderNo, int parentIndex) {

        public boolean hasParent() {
            return parentIndex >= 0;
        }
    }

    public record Result(Section.Source source, List<Entry> entries) {
    }

    /**
     * PDF 북마크가 있으면 그것을, 없으면 페이지별 제목 블록에서 추정한 것을 목차로 삼는다.
     * 어느 쪽이든 중복을 합치고 상한을 적용한 뒤 계층을 잇는다.
     */
    public static Result build(List<ParsedDocument.ParsedSection> bookmarks,
                                Map<Integer, List<PageBlock>> blocksByPage) {
        boolean fromBookmarks = !bookmarks.isEmpty();
        List<ParsedDocument.ParsedSection> candidates = fromBookmarks ? bookmarks : derive(blocksByPage);
        Section.Source source = fromBookmarks ? Section.Source.ORIGINAL : Section.Source.EXTRACTED;
        return new Result(source, link(trim(candidates)));
    }

    /**
     * 목록에 보여줄 한 줄 요약.
     *
     * <p>문장으로 끝나는 문단만 고른다. 길이만 보고 첫 문단을 집으면 발표자료에서는
     * 목차 네비게이션("Problem Insight Solution")이, 보고서에서는 표지의 제출일이 잡힌다.
     * 마땅한 문장이 없으면 비워 둔다 — 화면은 요약이 없어도 그린다.
     */
    public static String summarize(Map<Integer, List<PageBlock>> blocksByPage) {
        return blocksByPage.values().stream()
                .flatMap(List::stream)
                .filter(block -> "p".equals(block.kind()))
                .map(PageBlock::text)
                .filter(DocumentOutline::looksLikeSentence)
                .findFirst()
                .map(String::strip)
                .map(DocumentOutline::truncate)
                .orElse(null);
    }

    /** 페이지마다 첫 제목 블록 하나씩만 목차 후보로 올린다. 중복과 상한은 trim이 처리한다. */
    private static List<ParsedDocument.ParsedSection> derive(Map<Integer, List<PageBlock>> blocksByPage) {
        List<ParsedDocument.ParsedSection> sections = new ArrayList<>();
        for (Map.Entry<Integer, List<PageBlock>> entry : blocksByPage.entrySet()) {
            entry.getValue().stream()
                    .filter(block -> "h2".equals(block.kind()))
                    .findFirst()
                    .ifPresent(block -> sections.add(new ParsedDocument.ParsedSection(block.text(),
                            SUB_HEADING.matcher(block.text()).matches() ? 2 : 1, entry.getKey(), sections.size())));
        }
        return sections;
    }

    /**
     * 연속으로 같은 제목이 반복되면 하나로 합치고, 상한을 넘으면 자른다.
     *
     * <p>반복은 목차가 아니라 쪽마다 찍히는 머리글이다. 실제로 12쪽 문서의 목차 12개 중
     * 11개가 같은 머리글이었던 적이 있다.
     */
    private static List<ParsedDocument.ParsedSection> trim(List<ParsedDocument.ParsedSection> candidates) {
        List<ParsedDocument.ParsedSection> result = new ArrayList<>();
        String previousTitle = null;
        for (ParsedDocument.ParsedSection s : candidates) {
            if (result.size() >= MAX_SECTIONS) {
                break;
            }
            if (s.title() == null || s.title().isBlank() || s.title().equals(previousTitle)) {
                continue;
            }
            previousTitle = s.title();
            result.add(new ParsedDocument.ParsedSection(s.title(), s.level(), s.pageFrom(), result.size()));
        }
        return result;
    }

    /** 부모는 앞쪽에서 가장 가까운, 더 얕은 레벨의 항목이다. */
    private static List<Entry> link(List<ParsedDocument.ParsedSection> trimmed) {
        List<Entry> entries = new ArrayList<>();
        for (ParsedDocument.ParsedSection s : trimmed) {
            int parentIndex = -1;
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i).level() < s.level()) {
                    parentIndex = i;
                    break;
                }
            }
            entries.add(new Entry(s.title(), s.level(), s.pageFrom(), s.orderNo(), parentIndex));
        }
        return entries;
    }

    /**
     * 요약으로 쓸 만한 문장인지.
     *
     * <p>마침표로 끝난다고 다 문장은 아니다. 표지의 "제  출  일 2026. 08. 13." 도 마침표로
     * 끝나기 때문에, 마침표 앞이 글자인지까지 본다. 숫자로 끝나면 날짜나 번호 항목이다.
     */
    private static boolean looksLikeSentence(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.strip();
        if (text.length() < SUMMARY_MIN_LENGTH) {
            return false;
        }
        char last = text.charAt(text.length() - 1);
        if (last == '.' || last == '!' || last == '?') {
            return Character.isLetter(text.charAt(text.length() - 2));
        }
        return KOREAN_ENDING.matcher(text).matches();
    }

    private static String truncate(String text) {
        return text.length() <= SUMMARY_MAX_LENGTH ? text : text.substring(0, SUMMARY_MAX_LENGTH) + "…";
    }
}
