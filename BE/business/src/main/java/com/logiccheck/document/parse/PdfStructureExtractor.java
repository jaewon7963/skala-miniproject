package com.logiccheck.document.parse;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// PDFBox만으로 페이지 단위 텍스트/크기와 (있다면) 북마크 기반 목차만 뽑는다.
// 표/수치/주장 등 요소 단위 추출은 스펙에 상세 규칙이 없어 범위 밖으로 둔다.
@Component
public class PdfStructureExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfStructureExtractor.class);

    public ParsedDocument extract(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            List<ParsedDocument.ParsedPage> pages = extractPages(document);
            List<ParsedDocument.ParsedSection> sections = extractSections(document);
            return new ParsedDocument(document.getNumberOfPages(), pages, sections);
        }
    }

    private List<ParsedDocument.ParsedPage> extractPages(PDDocument document) throws IOException {
        List<ParsedDocument.ParsedPage> pages = new ArrayList<>();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String text = stripper.getText(document);
            pages.add(new ParsedDocument.ParsedPage(i + 1,
                    BigDecimal.valueOf(page.getMediaBox().getWidth()),
                    BigDecimal.valueOf(page.getMediaBox().getHeight()), text));
        }
        return pages;
    }

    private List<ParsedDocument.ParsedSection> extractSections(PDDocument document) {
        List<ParsedDocument.ParsedSection> sections = new ArrayList<>();
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline == null) {
            return sections;
        }
        int[] ordering = {0};
        walk(document, outline, 1, sections, ordering);
        return sections;
    }

    private void walk(PDDocument document, PDOutlineNode node, int level, List<ParsedDocument.ParsedSection> out,
                       int[] ordering) {
        for (PDOutlineItem item = node.getFirstChild(); item != null; item = item.getNextSibling()) {
            Integer pageFrom = resolvePageNo(document, item);
            out.add(new ParsedDocument.ParsedSection(item.getTitle(), level, pageFrom, ordering[0]++));
            walk(document, item, level + 1, out, ordering);
        }
    }

    private Integer resolvePageNo(PDDocument document, PDOutlineItem item) {
        try {
            PDPage page = item.findDestinationPage(document);
            return page == null ? null : document.getPages().indexOf(page) + 1;
        } catch (IOException e) {
            log.debug("목차 항목의 페이지를 찾지 못했습니다: {}", item.getTitle(), e);
            return null;
        }
    }
}
