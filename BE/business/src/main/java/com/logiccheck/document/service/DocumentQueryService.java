package com.logiccheck.document.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.document.dto.CountsResponse;
import com.logiccheck.document.dto.DocumentDetailResponse;
import com.logiccheck.document.dto.DocumentListResponse;
import com.logiccheck.document.dto.DocumentSort;
import com.logiccheck.document.dto.DocumentSummaryResponse;
import com.logiccheck.document.dto.PageResponse;
import com.logiccheck.document.dto.ParseStatusResponse;
import com.logiccheck.document.dto.Period;
import com.logiccheck.document.dto.SectionResponse;
import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.port.ReviewJobQueryPort;
import com.logiccheck.document.port.ReviewJobQueryPort.LatestJobView;
import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.repository.DocumentTagRepository;
import com.logiccheck.document.repository.PageRepository;
import com.logiccheck.document.repository.SectionRepository;
import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.tag.dto.TagResponse;

@Service
@Transactional(readOnly = true)
public class DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;
    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final ReviewJobQueryPort reviewJobQueryPort;

    public DocumentQueryService(DocumentRepository documentRepository, DocumentTagRepository documentTagRepository,
                                 PageRepository pageRepository, SectionRepository sectionRepository,
                                 ReviewJobQueryPort reviewJobQueryPort) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.reviewJobQueryPort = reviewJobQueryPort;
    }

    public DocumentListResponse list(Long ownerId, String status, String q, String period, String tagIdRaw,
                                      String sortRaw, int page, int size) {
        if (status != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "status"));
        }
        DocumentSort sort = DocumentSort.from(sortRaw);
        Period cutoffPeriod = Period.from(period);
        Long tagId = parseTagId(tagIdRaw);

        List<Specification<Document>> specs = new ArrayList<>();
        specs.add(DocumentSpecifications.ownerIs(ownerId));
        specs.add(DocumentSpecifications.notDeleted());
        addIfNotNull(specs, DocumentSpecifications.titleContains(q));
        addIfNotNull(specs, DocumentSpecifications.updatedAfter(cutoffPeriod.cutoff()));
        addIfNotNull(specs, DocumentSpecifications.hasTag(tagId));
        Specification<Document> spec = Specification.allOf(specs);

        Sort springSort = sort == DocumentSort.NAME_ASC
                ? Sort.by(Sort.Direction.ASC, "title")
                : Sort.by(Sort.Direction.DESC, "updatedAt");

        var result = documentRepository.findAll(spec, PageRequest.of(page - 1, size, springSort));

        List<Long> docIds = result.getContent().stream().map(Document::getId).toList();
        Map<Long, LatestJobView> jobs = reviewJobQueryPort.findLatestByDocumentIds(docIds);
        Map<Long, List<TagResponse>> tagsByDoc = tagsByDocumentId(docIds);

        List<DocumentSummaryResponse> items = result.getContent().stream()
                .map(d -> DocumentSummaryResponse.of(d, DisplayStatusCalculator.calculate(d.getParseStatus(),
                        jobs.get(d.getId())), tagsByDoc.getOrDefault(d.getId(), List.of())))
                .toList();

        return new DocumentListResponse(items, result.getTotalElements(), page, size, counts(ownerId));
    }

    public CountsResponse counts(Long ownerId) {
        List<DocumentRepository.DocIdStatus> all = documentRepository.findByOwnerIdAndDeletedAtIsNull(ownerId);
        Map<Long, LatestJobView> jobs = reviewJobQueryPort.findLatestByDocumentIds(
                all.stream().map(DocumentRepository.DocIdStatus::getId).toList());

        long total = all.size();
        long inReview = 0;
        long completed = 0;
        long failed = 0;
        for (DocumentRepository.DocIdStatus d : all) {
            String displayStatus = DisplayStatusCalculator.calculate(d.getParseStatus(), jobs.get(d.getId()));
            if (DisplayStatusCalculator.isFailed(displayStatus)) {
                failed++;
            } else if ("IN_REVIEW".equals(displayStatus)) {
                inReview++;
            } else if ("COMPLETED".equals(displayStatus)) {
                completed++;
            }
        }
        return new CountsResponse(total, inReview, completed, failed);
    }

    public DocumentDetailResponse detail(Long ownerId, Long documentId) {
        Document document = getOwnedOrThrow(documentId, ownerId);
        LatestJobView job = reviewJobQueryPort.findLatestByDocumentIds(List.of(documentId)).get(documentId);
        List<TagResponse> tags = documentTagRepository.findByDocument_Id(documentId).stream()
                .map(dt -> TagResponse.from(dt.getTag()))
                .toList();
        return DocumentDetailResponse.of(document, DisplayStatusCalculator.calculate(document.getParseStatus(), job),
                tags);
    }

    public Document getForDownload(Long ownerId, Long documentId) {
        return getOwnedOrThrow(documentId, ownerId);
    }

    public ParseStatusResponse parseStatus(Long ownerId, Long documentId) {
        return ParseStatusResponse.from(getOwnedOrThrow(documentId, ownerId));
    }

    public List<SectionResponse> sections(Long ownerId, Long documentId) {
        getOwnedOrThrow(documentId, ownerId);
        List<Section> flat = sectionRepository.findByDocument_IdOrderByOrderNoAsc(documentId);
        return buildTree(flat);
    }

    public List<PageResponse> pages(Long ownerId, Long documentId, Integer from, Integer to) {
        getOwnedOrThrow(documentId, ownerId);
        return pageRepository.findByDocument_IdOrderByPageNoAsc(documentId).stream()
                .filter(p -> from == null || p.getPageNo() >= from)
                .filter(p -> to == null || p.getPageNo() <= to)
                .map(PageResponse::from)
                .toList();
    }

    Document getOwnedOrThrow(Long documentId, Long ownerId) {
        return documentRepository.findByIdAndOwnerIdAndDeletedAtIsNull(documentId, ownerId)
                .orElseThrow(() -> {
                    if (documentRepository.existsByIdAndDeletedAtIsNull(documentId)) {
                        return new BusinessException(ErrorCode.FORBIDDEN);
                    }
                    return new BusinessException(ErrorCode.NOT_FOUND);
                });
    }

    private Map<Long, List<TagResponse>> tagsByDocumentId(List<Long> docIds) {
        if (docIds.isEmpty()) {
            return Map.of();
        }
        return documentTagRepository.findRowsByDocumentIdIn(docIds).stream()
                .collect(Collectors.groupingBy(DocumentTagRepository.DocumentTagRow::getDocumentId,
                        Collectors.mapping(row -> TagResponse.from(row.getTag()), Collectors.toList())));
    }

    private void addIfNotNull(List<Specification<Document>> specs, Specification<Document> spec) {
        if (spec != null) {
            specs.add(spec);
        }
    }

    private Long parseTagId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "tagId"));
        }
    }

    private List<SectionResponse> buildTree(List<Section> flat) {
        Map<Long, List<Section>> byParent = flat.stream()
                .filter(s -> s.getParent() != null)
                .collect(Collectors.groupingBy(s -> s.getParent().getId(), LinkedHashMap::new, Collectors.toList()));

        List<Section> roots = flat.stream().filter(s -> s.getParent() == null).toList();
        List<SectionResponse> result = new ArrayList<>();
        for (Section s : roots) {
            result.add(SectionResponse.withChildren(s, buildChildren(s.getId(), byParent)));
        }
        return result;
    }

    private List<SectionResponse> buildChildren(Long parentId, Map<Long, List<Section>> byParent) {
        List<Section> children = byParent.getOrDefault(parentId, List.of());
        List<SectionResponse> result = new ArrayList<>();
        for (Section c : children) {
            result.add(SectionResponse.withChildren(c, buildChildren(c.getId(), byParent)));
        }
        return result;
    }
}
