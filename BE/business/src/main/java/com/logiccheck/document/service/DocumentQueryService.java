package com.logiccheck.document.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.document.dto.CountsResponse;
import com.logiccheck.document.dto.DocumentListResponse;
import com.logiccheck.document.dto.DocumentResponse;
import com.logiccheck.document.dto.DocumentSort;
import com.logiccheck.document.dto.PageResponse;
import com.logiccheck.document.dto.ParseStatusResponse;
import com.logiccheck.document.dto.Period;
import com.logiccheck.document.dto.SectionResponse;
import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.Section;
import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.repository.DocumentTagRepository;
import com.logiccheck.document.repository.PageRepository;
import com.logiccheck.document.repository.SectionRepository;
import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.port.ReviewJobQueryPort;
import com.logiccheck.review.port.ReviewJobQueryPort.LatestJobView;
import com.logiccheck.tag.entity.Tag;
import com.logiccheck.tag.repository.TagRepository;

@Service
@Transactional(readOnly = true)
public class DocumentQueryService {

    private static final String STATUS_ALL = "ALL";

    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;
    private final PageRepository pageRepository;
    private final SectionRepository sectionRepository;
    private final TagRepository tagRepository;
    private final ReviewJobQueryPort reviewJobQueryPort;

    public DocumentQueryService(DocumentRepository documentRepository, DocumentTagRepository documentTagRepository,
                                 PageRepository pageRepository, SectionRepository sectionRepository,
                                 TagRepository tagRepository, ReviewJobQueryPort reviewJobQueryPort) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.tagRepository = tagRepository;
        this.reviewJobQueryPort = reviewJobQueryPort;
    }

    /**
     * 상태는 파싱 상태와 분석 작업을 합쳐 만든 파생 값이라 SQL WHERE 로 거를 수 없다.
     * 그래서 검색·기간·태그까지는 DB에서 좁히고, 상태 필터와 페이지 자르기는 메모리에서 한다.
     * 소유자 한 명의 문서 수를 다루는 화면이라 이 정도로 충분하다.
     */
    public DocumentListResponse list(Long ownerId, String status, String q, String period, String tagRaw,
                                      String sortRaw, int page, int size) {
        DocumentSort sort = DocumentSort.from(sortRaw);
        Period cutoffPeriod = Period.from(period);

        List<Specification<Document>> specs = new ArrayList<>();
        specs.add(DocumentSpecifications.ownerIs(ownerId));
        specs.add(DocumentSpecifications.notDeleted());
        addIfNotNull(specs, DocumentSpecifications.titleContains(q));
        addIfNotNull(specs, DocumentSpecifications.updatedAfter(cutoffPeriod.cutoff()));
        addIfNotNull(specs, DocumentSpecifications.hasTag(resolveTagId(tagRaw)));

        List<Document> matched = documentRepository.findAll(Specification.allOf(specs));
        List<Long> docIds = matched.stream().map(Document::getId).toList();
        Map<Long, LatestJobView> jobs = reviewJobQueryPort.findLatestByDocumentIds(docIds);
        Map<Long, List<String>> tagsByDoc = tagNamesByDocumentId(docIds);

        List<DocumentResponse> all = matched.stream()
                .map(d -> toResponse(d, jobs.get(d.getId()), tagsByDoc.getOrDefault(d.getId(), List.of())))
                .filter(d -> isAll(status) || d.status().equals(status))
                .sorted(comparator(sort))
                .toList();

        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new DocumentListResponse(all.subList(from, to), all.size(), page, size, counts(ownerId));
    }

    public CountsResponse counts(Long ownerId) {
        List<DocumentRepository.DocIdStatus> all = documentRepository.findByOwnerIdAndDeletedAtIsNull(ownerId);
        Map<Long, LatestJobView> jobs = reviewJobQueryPort.findLatestByDocumentIds(
                all.stream().map(DocumentRepository.DocIdStatus::getId).toList());

        Map<String, Long> byStatus = all.stream()
                .map(d -> DisplayStatusCalculator.calculate(d.getParseStatus(), jobs.get(d.getId())))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return new CountsResponse(all.size(),
                byStatus.getOrDefault(DisplayStatusCalculator.IDLE, 0L),
                byStatus.getOrDefault(DisplayStatusCalculator.PARSING, 0L),
                byStatus.getOrDefault(DisplayStatusCalculator.ANALYZING, 0L),
                byStatus.getOrDefault(DisplayStatusCalculator.REVIEWING, 0L),
                byStatus.getOrDefault(DisplayStatusCalculator.DONE, 0L),
                byStatus.getOrDefault(DisplayStatusCalculator.FAILED, 0L));
    }

    public DocumentResponse detail(Long ownerId, Long documentId) {
        Document document = getOwnedOrThrow(documentId, ownerId);
        return describe(document);
    }

    /** 문서를 이미 확보한 호출자(업로드·수정)가 같은 모양의 응답을 만들 때 쓴다. */
    public DocumentResponse describe(Document document) {
        Long documentId = document.getId();
        LatestJobView job = reviewJobQueryPort.findLatestByDocumentIds(List.of(documentId)).get(documentId);
        List<String> tags = documentTagRepository.findByDocument_Id(documentId).stream()
                .map(dt -> dt.getTag().getName())
                .toList();
        return toResponse(document, job, tags);
    }

    public Document getForDownload(Long ownerId, Long documentId) {
        return getOwnedOrThrow(documentId, ownerId);
    }

    public ParseStatusResponse parseStatus(Long ownerId, Long documentId) {
        return ParseStatusResponse.from(getOwnedOrThrow(documentId, ownerId));
    }

    public List<SectionResponse> sections(Long ownerId, Long documentId) {
        getOwnedOrThrow(documentId, ownerId);
        return buildTree(sectionRepository.findByDocument_IdOrderByOrderNoAsc(documentId));
    }

    public List<PageResponse> pages(Long ownerId, Long documentId, Integer from, Integer to) {
        getOwnedOrThrow(documentId, ownerId);
        return pageRepository.findByDocument_IdOrderByPageNoAsc(documentId).stream()
                .filter(p -> from == null || p.getPageNo() >= from)
                .filter(p -> to == null || p.getPageNo() <= to)
                .map(PageResponse::from)
                .toList();
    }

    public Document getOwnedOrThrow(Long documentId, Long ownerId) {
        return documentRepository.findByIdAndOwnerIdAndDeletedAtIsNull(documentId, ownerId)
                .orElseThrow(() -> {
                    if (documentRepository.existsByIdAndDeletedAtIsNull(documentId)) {
                        return new BusinessException(ErrorCode.FORBIDDEN);
                    }
                    return new BusinessException(ErrorCode.NOT_FOUND);
                });
    }

    private DocumentResponse toResponse(Document document, LatestJobView job, List<String> tags) {
        String status = DisplayStatusCalculator.calculate(document.getParseStatus(), job);
        return DocumentResponse.of(document, status, tags, job == null ? null : job.jobId());
    }

    private boolean isAll(String status) {
        return status == null || status.isBlank() || STATUS_ALL.equals(status);
    }

    private Comparator<DocumentResponse> comparator(DocumentSort sort) {
        return sort == DocumentSort.NAME_ASC
                ? Comparator.comparing(DocumentResponse::name)
                : Comparator.comparing(DocumentResponse::updatedAt).reversed();
    }

    /** 화면은 태그를 이름으로 다루므로 코드·이름·숫자 id 어느 쪽이 와도 받아준다. 못 찾으면 필터를 걸지 않는다. */
    private Long resolveTagId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return tagRepository.findAllByOrderByOrderNoAsc().stream()
                .filter(tag -> raw.equalsIgnoreCase(tag.getCode()) || raw.equals(tag.getName())
                        || raw.equals(String.valueOf(tag.getId())))
                .map(Tag::getId)
                .findFirst()
                .orElse(null);
    }

    private Map<Long, List<String>> tagNamesByDocumentId(List<Long> docIds) {
        if (docIds.isEmpty()) {
            return Map.of();
        }
        return documentTagRepository.findRowsByDocumentIdIn(docIds).stream()
                .collect(Collectors.groupingBy(DocumentTagRepository.DocumentTagRow::getDocumentId,
                        Collectors.mapping(row -> row.getTag().getName(), Collectors.toList())));
    }

    private void addIfNotNull(List<Specification<Document>> specs, Specification<Document> spec) {
        if (spec != null) {
            specs.add(spec);
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
