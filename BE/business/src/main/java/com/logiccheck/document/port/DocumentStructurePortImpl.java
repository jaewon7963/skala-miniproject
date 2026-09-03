package com.logiccheck.document.port;

import java.util.List;

import org.springframework.stereotype.Component;

import com.logiccheck.document.entity.Page;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.repository.PageRepository;
import com.logiccheck.document.repository.SectionRepository;

@Component
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
