package com.logiccheck.document.port;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.document.entity.Page;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.repository.PageRepository;
import com.logiccheck.document.repository.SectionRepository;

// open-in-view를 끈 상태라 지연 로딩 필드를 건드리려면 조회 자체가 트랜잭션 안에 있어야 한다.
@Component
@Transactional(readOnly = true)
public class DocumentStructurePortImpl implements DocumentStructurePort {

    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;

    public DocumentStructurePortImpl(PageRepository pageRepository, SectionRepository sectionRepository) {
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    public List<PageView> findPages(Long documentId) {
        return pageRepository.findByDocument_IdOrderByPageNoAsc(documentId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<SectionView> findSections(Long documentId) {
        return sectionRepository.findByDocument_IdOrderByOrderNoAsc(documentId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<ElementView> findElements(Long documentId) {
        return List.of();
    }

    @Override
    public List<PageContentView> findPageContents(Long documentId) {
        return pageRepository.findWithSectionByDocumentId(documentId).stream()
                .map(this::toContentView)
                .toList();
    }

    private PageContentView toContentView(Page p) {
        Section section = p.getSection();
        return new PageContentView(p.getPageNo(),
                section == null ? null : section.getId(),
                section == null ? null : section.getTitle(),
                p.getBlocks() == null ? List.of() : p.getBlocks());
    }

    private PageView toView(Page p) {
        Double width = p.getWidth() == null ? null : p.getWidth().doubleValue();
        Double height = p.getHeight() == null ? null : p.getHeight().doubleValue();
        return new PageView(p.getPageNo(), width, height, p.getTextLayer());
    }

    private SectionView toView(Section s) {
        Long parentId = s.getParent() != null ? s.getParent().getId() : null;
        return new SectionView(s.getId(), parentId, s.getTitle(), s.getLevel(), s.getPageFrom(), s.getPageTo(),
                s.getOrderNo(), s.getSource().name());
    }
}
