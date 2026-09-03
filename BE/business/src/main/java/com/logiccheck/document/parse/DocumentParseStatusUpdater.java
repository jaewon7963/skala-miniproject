package com.logiccheck.document.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.Page;
import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.repository.PageRepository;
import com.logiccheck.document.repository.SectionRepository;

// 파싱 단계별로 별도 트랜잭션을 커밋해야 폴링 중인 GET .../parse-status 요청에
// PARSING/EXTRACTING 중간 상태가 실제로 보인다 (하나의 큰 트랜잭션이면 끝날 때까지 안 보임).
@Component
public class DocumentParseStatusUpdater {

    /** "2.1 목표 시장 규모"처럼 소수점이 있으면 하위 목차로 본다. */
    private static final Pattern SUB_HEADING = Pattern.compile("^\\d+\\.\\d+.*");
    private static final int MAX_DERIVED_SECTIONS = 40;
    private static final int SUMMARY_MAX_LENGTH = 120;

    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final PageBlockExtractor blockExtractor;

    public DocumentParseStatusUpdater(DocumentRepository documentRepository, PageRepository pageRepository,
                                       SectionRepository sectionRepository, PageBlockExtractor blockExtractor) {
        this.documentRepository = documentRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.blockExtractor = blockExtractor;
    }

    @Transactional
    public void markParsing(Long documentId) {
        documentRepository.findById(documentId).ifPresent(Document::markParsing);
    }

    @Transactional
    public void markExtracting(Long documentId) {
        documentRepository.findById(documentId).ifPresent(Document::markExtracting);
    }

    @Transactional
    public void completeParsing(Long documentId, ParsedDocument parsed) {
        Document document = documentRepository.findById(documentId).orElseThrow();

        Map<Integer, List<PageBlock>> blocksByPage = new LinkedHashMap<>();
        for (ParsedDocument.ParsedPage p : parsed.pages()) {
            blocksByPage.put(p.pageNo(), blockExtractor.extract(p.pageNo(), p.text()));
        }

        // PDF 북마크가 있으면 그대로 쓰고, 없으면(대부분의 사업계획서가 그렇다) 페이지별 제목 블록에서 만든다.
        List<ParsedDocument.ParsedSection> outline = parsed.sections().isEmpty()
                ? deriveSections(blocksByPage)
                : parsed.sections();

        List<Section> sections = new ArrayList<>();
        for (ParsedDocument.ParsedSection s : outline) {
            sections.add(sectionRepository.save(Section.of(document, null, Section.Source.ORIGINAL, s.title(),
                    s.level(), s.pageFrom(), null, s.orderNo())));
        }

        for (ParsedDocument.ParsedPage p : parsed.pages()) {
            Page page = pageRepository.save(Page.of(document, p.pageNo(), p.width(), p.height(), p.text()));
            page.attach(blocksByPage.get(p.pageNo()), sectionOf(sections, p.pageNo()));
        }

        document.completeParsing(parsed.pageCount());
        document.describe(summarize(blocksByPage));
    }

    @Transactional
    public void markFailed(Long documentId) {
        documentRepository.findById(documentId).ifPresent(Document::markFailed);
    }

    /** 페이지마다 첫 제목 블록 하나씩만 목차로 올린다. 페이지 수만큼 항목이 늘어나는 것을 막는다. */
    private List<ParsedDocument.ParsedSection> deriveSections(Map<Integer, List<PageBlock>> blocksByPage) {
        List<ParsedDocument.ParsedSection> sections = new ArrayList<>();
        for (Map.Entry<Integer, List<PageBlock>> entry : blocksByPage.entrySet()) {
            if (sections.size() >= MAX_DERIVED_SECTIONS) {
                break;
            }
            entry.getValue().stream()
                    .filter(block -> "h2".equals(block.kind()))
                    .findFirst()
                    .ifPresent(block -> sections.add(new ParsedDocument.ParsedSection(block.text(),
                            SUB_HEADING.matcher(block.text()).matches() ? 2 : 1, entry.getKey(), sections.size())));
        }
        return sections;
    }

    /** 페이지가 속한 섹션 = 시작 페이지가 그 페이지 이하인 마지막 섹션. */
    private Section sectionOf(List<Section> sections, int pageNo) {
        Section current = null;
        for (Section section : sections) {
            if (section.getPageFrom() != null && section.getPageFrom() <= pageNo) {
                current = section;
            }
        }
        return current;
    }

    private String summarize(Map<Integer, List<PageBlock>> blocksByPage) {
        String first = blocksByPage.values().stream()
                .flatMap(List::stream)
                .filter(block -> "p".equals(block.kind()) && block.text() != null && block.text().length() > 10)
                .map(PageBlock::text)
                .findFirst()
                .orElse("업로드 완료 · 분석 대기");
        return first.length() <= SUMMARY_MAX_LENGTH ? first : first.substring(0, SUMMARY_MAX_LENGTH) + "…";
    }
}
