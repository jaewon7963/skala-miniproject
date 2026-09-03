package com.logiccheck.document.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        DocumentOutline.Result outline = DocumentOutline.build(parsed.sections(), blocksByPage);
        List<Section> sections = save(document, outline);

        for (ParsedDocument.ParsedPage p : parsed.pages()) {
            Page page = pageRepository.save(Page.of(document, p.pageNo(), p.width(), p.height(), p.text()));
            page.attach(blocksByPage.get(p.pageNo()), sectionOf(sections, p.pageNo()));
        }

        document.completeParsing(parsed.pageCount());
        document.describe(DocumentOutline.summarize(blocksByPage));
    }

    @Transactional
    public void markFailed(Long documentId) {
        documentRepository.findById(documentId).ifPresent(Document::markFailed);
    }

    /** 목차를 순서대로 저장하면서 계층을 잇는다. 부모는 이미 저장된 앞쪽 항목이라 인덱스로 찾는다. */
    private List<Section> save(Document document, DocumentOutline.Result outline) {
        List<Section> saved = new ArrayList<>();
        for (DocumentOutline.Entry entry : outline.entries()) {
            Section parent = entry.hasParent() ? saved.get(entry.parentIndex()) : null;
            saved.add(sectionRepository.save(Section.of(document, parent, outline.source(), entry.title(),
                    entry.level(), entry.pageFrom(), null, entry.orderNo())));
        }
        return saved;
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
}
