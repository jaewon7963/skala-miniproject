package com.logiccheck.document.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.logiccheck.document.entity.PageBlock;

/**
 * PDFBox가 뽑아준 페이지 텍스트를 원문 뷰어용 블록 배열로 나눈다.
 *
 * <p>PDF에는 "여기부터 표"라는 구조 정보가 없어서 규칙으로 추정한다.
 * 공백이 두 칸 이상으로 열이 갈리는 줄이 연달아 나오면 표로, 번호로 시작하거나
 * 짧고 문장부호로 끝나지 않는 줄은 제목으로, 나머지는 문단으로 본다.
 * 잘못 나눠도 화면은 그대로 렌더링되므로 실패보다 과소추정 쪽으로 기울여 둔다.
 */
@Component
public class PageBlockExtractor {

    /** "1." "2.3" "III." 처럼 번호로 시작하는 제목 줄. */
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\d+(\\.\\d+)*[.)]?\\s+\\S.{0,60}$");
    private static final Pattern TABLE_CAPTION = Pattern.compile("^[\\[(]?\\s*(표|Table|TABLE)\\s?[\\d\\-.]*.*");
    private static final Pattern COLUMN_GAP = Pattern.compile("\\s{2,}");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final int HEADING_MAX_LENGTH = 40;
    private static final int MIN_TABLE_ROWS = 2;
    private static final int MIN_TABLE_COLUMNS = 3;

    public List<PageBlock> extract(int pageNo, String rawText) {
        List<String> lines = normalize(rawText);
        if (lines.isEmpty()) {
            return List.of(PageBlock.text(blockId(pageNo, 1), "figure", "이미지 · 도표 영역 (텍스트 레이어 없음)"));
        }

        List<PageBlock> blocks = new ArrayList<>();
        List<String> paragraph = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int tableEnd = tableRunEnd(lines, i);
            if (tableEnd > i) {
                String caption = takeCaption(blocks, paragraph);
                flushParagraph(blocks, paragraph);
                blocks.add(toTable(caption, lines.subList(i, tableEnd)));
                i = tableEnd - 1;
                continue;
            }

            String line = lines.get(i);
            if (isHeading(line)) {
                flushParagraph(blocks, paragraph);
                blocks.add(PageBlock.text(null, "h2", line));
                continue;
            }
            paragraph.add(line);
        }
        flushParagraph(blocks, paragraph);
        return numbered(pageNo, blocks);
    }

    /**
     * 식별자는 다 나눈 뒤에 한 번에 붙인다. 만드는 도중에 붙이면 캡션을 표 쪽으로
     * 옮길 때 번호가 비어 버려서, 앵커가 가리키는 값이 문서마다 들쭉날쭉해진다.
     */
    private List<PageBlock> numbered(int pageNo, List<PageBlock> blocks) {
        return java.util.stream.IntStream.range(0, blocks.size())
                .mapToObj(i -> {
                    PageBlock block = blocks.get(i);
                    return new PageBlock(blockId(pageNo, i + 1), block.kind(), block.text(), block.caption(),
                            block.head(), block.rows());
                })
                .toList();
    }

    /** 표 바로 앞 줄이 "[표 5-1] ..." 형태면 캡션으로 떼어 쓴다. */
    private String takeCaption(List<PageBlock> blocks, List<String> paragraph) {
        if (!paragraph.isEmpty()) {
            String last = paragraph.get(paragraph.size() - 1);
            return TABLE_CAPTION.matcher(last).matches() ? paragraph.remove(paragraph.size() - 1) : null;
        }
        if (blocks.isEmpty()) {
            return null;
        }
        PageBlock last = blocks.get(blocks.size() - 1);
        if (last.isTable() || last.text() == null || !TABLE_CAPTION.matcher(last.text()).matches()) {
            return null;
        }
        blocks.remove(blocks.size() - 1);
        return last.text();
    }

    private void flushParagraph(List<PageBlock> blocks, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        blocks.add(PageBlock.text(null, "p", String.join(" ", paragraph)));
        paragraph.clear();
    }

    /** i번째 줄부터 이어지는 표 구간의 끝(exclusive). 표가 아니면 i를 그대로 돌려준다. */
    private int tableRunEnd(List<String> lines, int start) {
        int end = start;
        while (end < lines.size() && isTableRow(lines.get(end))) {
            end++;
        }
        return end - start >= MIN_TABLE_ROWS ? end : start;
    }

    private boolean isTableRow(String line) {
        String[] cells = COLUMN_GAP.split(line.trim());
        return cells.length >= MIN_TABLE_COLUMNS && HAS_DIGIT.matcher(line).find();
    }

    private PageBlock toTable(String caption, List<String> rows) {
        List<List<String>> cells = rows.stream().map(this::splitRow).toList();
        int columns = cells.stream().mapToInt(List::size).max().orElse(0);
        List<List<String>> padded = cells.stream().map(row -> pad(row, columns)).toList();
        return PageBlock.table(null, caption, padded.get(0), padded.subList(1, padded.size()));
    }

    private List<String> splitRow(String line) {
        return Arrays.stream(COLUMN_GAP.split(line.trim())).map(String::trim).toList();
    }

    private List<String> pad(List<String> row, int columns) {
        if (row.size() >= columns) {
            return row.subList(0, columns);
        }
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < columns) {
            padded.add("");
        }
        return padded;
    }

    private boolean isHeading(String line) {
        if (NUMBERED_HEADING.matcher(line).matches()) {
            return true;
        }
        return line.length() <= HEADING_MAX_LENGTH && !endsLikeSentence(line);
    }

    private boolean endsLikeSentence(String line) {
        char last = line.charAt(line.length() - 1);
        return last == '.' || last == '。' || last == '?' || last == '!' || last == ',' || line.endsWith("다");
    }

    private List<String> normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawText.split("\\R"))
                .map(line -> line.replace('\u00A0', ' ').stripTrailing())
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String blockId(int pageNo, int index) {
        return "b-" + pageNo + "-" + index;
    }
}
