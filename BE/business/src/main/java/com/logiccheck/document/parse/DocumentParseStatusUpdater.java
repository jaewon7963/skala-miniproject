package com.logiccheck.document.parse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.Page;
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

    public DocumentParseStatusUpdater(DocumentRepository documentRepository, PageRepository pageRepository,
                                       SectionRepository sectionRepository) {
        this.documentRepository = documentRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
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

        for (ParsedDocument.ParsedPage p : parsed.pages()) {
            pageRepository.save(Page.of(document, p.pageNo(), p.width(), p.height(), p.text()));
        }
        for (ParsedDocument.ParsedSection s : parsed.sections()) {
            sectionRepository.save(Section.of(document, null, Section.Source.ORIGINAL, s.title(), s.level(),
                    s.pageFrom(), null, s.orderNo()));
        }

        document.completeParsing(parsed.pageCount());
    }

    @Transactional
    public void markFailed(Long documentId) {
        documentRepository.findById(documentId).ifPresent(Document::markFailed);
    }
}
