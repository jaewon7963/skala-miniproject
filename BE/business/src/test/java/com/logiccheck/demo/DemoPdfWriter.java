package com.logiccheck.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * 시연용 사업계획서 PDF를 만든다. 산출물은 저장소에 커밋하므로 평소에는 돌릴 일이 없고,
 * 본문({@link DemoPdfContent})을 고쳤을 때만 {@code ./gradlew generateDemoPdf} 로 다시 만든다.
 *
 * <p>PDF에는 "여기부터 표"라는 구조 정보가 없어서 {@code PageBlockExtractor}는 공백 두 칸
 * 이상으로 열이 갈리는 줄을 표로 추정한다. 그래서 표 한 행을 셀별로 나눠 그리지 않고
 * <b>공백을 실제로 넣은 문자열 한 덩어리</b>로 그린다. 그래야 추출한 텍스트에 공백이
 * 그대로 남아 표로 읽힌다. 열 맞춤은 폰트 폭을 재서 공백 개수로 맞춘다.
 *
 * <p>목차는 PDF 북마크에서만 나온다({@code PdfStructureExtractor.extractSections}). 북마크가
 * 없으면 목차 패널이 빈 채로 뜨므로 페이지마다 하나씩 넣는다.
 *
 * <p>한글 폰트는 나눔고딕(SIL Open Font License 1.1)을 쓴다. OFL은 PDF 임베딩을 허용한다.
 */
public final class DemoPdfWriter {

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 55;
    private static final float BODY_SIZE = 9.5f;
    private static final float HEADING_SIZE = 11.5f;
    private static final float COVER_SIZE = 16f;
    private static final float LEADING = 17.5f;

    /** {@code PageBlockExtractor.isHeading} 와 같은 기준. 본문이 제목으로 오인되는 것을 막는다. */
    private static final int HEADING_MAX_LENGTH = 40;
    private static final Pattern SUB_HEADING = Pattern.compile("^\\d+\\.\\d+\\s.*");
    private static final Pattern TABLE_CAPTION = Pattern.compile("^[\\[(]?\\s*(표|Table|TABLE)\\s?[\\d\\-.]*.*");
    private static final Pattern COLUMN_GAP = Pattern.compile("\\s{2,}");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    private DemoPdfWriter() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("사용법: DemoPdfWriter <한글폰트.ttf> <출력.pdf> [출력2.pdf ...]");
        }
        byte[] pdf = build(Path.of(args[0]));
        for (int i = 1; i < args.length; i++) {
            Path out = Path.of(args[i]);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            Files.write(out, pdf);
            System.out.println("생성 완료: " + out.toAbsolutePath() + " (" + pdf.length + " bytes)");
        }
    }

    public static byte[] build(Path fontFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font font = PDType0Font.load(document, fontFile.toFile());

            PDDocumentOutline outline = new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(outline);

            boolean firstPage = true;
            PDOutlineItem lastTopLevel = null;
            for (DemoPdfContent.Page content : DemoPdfContent.pages()) {
                PDPage page = new PDPage(PAGE_SIZE);
                document.addPage(page);
                writePage(document, page, font, content, firstPage);
                lastTopLevel = addBookmark(outline, lastTopLevel, page, content);
                firstPage = false;
            }
            outline.openNode();

            document.getDocumentInformation().setTitle(DemoPdfContent.TITLE);
            document.getDocumentInformation().setAuthor("주식회사 로직체크");
            document.getDocumentInformation().setProducer("BizXray DemoPdfWriter");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void writePage(PDDocument document, PDPage page, PDType0Font font,
                                   DemoPdfContent.Page content, boolean cover) throws IOException {
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            float y = PAGE_SIZE.getHeight() - MARGIN;
            boolean firstLine = true;
            for (DemoPdfContent.Line line : content.lines()) {
                if (line.kind() == DemoPdfContent.Line.Kind.BLANK) {
                    y -= LEADING * 0.6f;
                    continue;
                }
                float size = sizeOf(line, cover && firstLine);
                String text = render(line, font, size);
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(text);
                stream.endText();
                y -= LEADING;
                firstLine = false;
            }
        }
    }

    private static float sizeOf(DemoPdfContent.Line line, boolean cover) {
        if (cover) {
            return COVER_SIZE;
        }
        return switch (line.kind()) {
            case HEADING -> HEADING_SIZE;
            case CAPTION -> BODY_SIZE + 0.5f;
            default -> BODY_SIZE;
        };
    }

    /** 그릴 문자열을 만들면서, 파서가 의도한 종류로 읽어 줄지까지 여기서 확인한다. */
    private static String render(DemoPdfContent.Line line, PDType0Font font, float size) throws IOException {
        return switch (line.kind()) {
            case ROW -> checkedRow(tableRow(line, font, size));
            case CAPTION -> checkedCaption(line.text());
            case BODY -> checkedBody(line.text());
            default -> line.text();
        };
    }

    private static String checkedBody(String text) {
        if (text.length() <= HEADING_MAX_LENGTH && !endsLikeSentence(text)) {
            throw new IllegalStateException(
                    "본문 줄이 제목으로 읽힙니다(40자 이하이면서 문장부호로 끝나지 않음): \"" + text + "\"");
        }
        return text;
    }

    private static String checkedCaption(String text) {
        if (!TABLE_CAPTION.matcher(text).matches()) {
            throw new IllegalStateException("표 캡션 형식이 아닙니다(\"[표 N] ...\"): \"" + text + "\"");
        }
        return text;
    }

    private static String checkedRow(String row) {
        String[] cells = COLUMN_GAP.split(row.trim());
        if (cells.length < 3) {
            throw new IllegalStateException("표 행의 열이 3개 미만이라 표로 읽히지 않습니다: \"" + row + "\"");
        }
        if (!HAS_DIGIT.matcher(row).find()) {
            throw new IllegalStateException("표 행에 숫자가 없어 표로 읽히지 않습니다: \"" + row + "\"");
        }
        return row;
    }

    /** {@code PageBlockExtractor.endsLikeSentence} 와 같은 기준. */
    private static boolean endsLikeSentence(String line) {
        char last = line.charAt(line.length() - 1);
        return last == '.' || last == '。' || last == '?' || last == '!' || last == ',' || line.endsWith("다");
    }

    /** 열 시작 위치가 맞도록 공백을 채운다. 어떤 경우에도 공백 두 칸 이상은 보장한다. */
    private static String tableRow(DemoPdfContent.Line line, PDType0Font font, float size) throws IOException {
        List<String> cells = line.cells();
        float[] columns = line.columns();
        StringBuilder row = new StringBuilder();
        float target = 0;
        for (int i = 0; i < cells.size(); i++) {
            row.append(cells.get(i));
            if (i == cells.size() - 1) {
                break;
            }
            target += columns[i];
            row.append("  ");
            while (width(font, size, row.toString()) < target) {
                row.append(' ');
            }
        }
        return row.toString();
    }

    private static float width(PDType0Font font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    /**
     * 목차는 북마크에서만 나온다({@code PdfStructureExtractor}). "2.1"처럼 소수점이 있는 제목은
     * 바로 앞 대제목의 자식으로 걸어 2단 목차를 만든다.
     *
     * @return 다음 페이지가 자식으로 붙을 대제목 항목
     */
    private static PDOutlineItem addBookmark(PDDocumentOutline outline, PDOutlineItem lastTopLevel, PDPage page,
                                              DemoPdfContent.Page content) {
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(content.bookmark());
        item.setDestination(page);

        if (SUB_HEADING.matcher(content.bookmark()).matches() && lastTopLevel != null) {
            lastTopLevel.addLast(item);
            lastTopLevel.openNode();
            return lastTopLevel;
        }
        outline.addLast(item);
        return item;
    }

}
