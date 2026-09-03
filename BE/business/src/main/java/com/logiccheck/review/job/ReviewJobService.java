package com.logiccheck.review.job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.ai.ReviewAiAnswer;
import com.logiccheck.ai.ReviewAiClient;
import com.logiccheck.ai.ReviewAiQuestion;
import com.logiccheck.ai.ReviewAiResult;
import com.logiccheck.auth.service.AuthService;
import com.logiccheck.document.port.DocumentQueryPort;
import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.document.port.DocumentStructurePort;
import com.logiccheck.document.port.DocumentStructurePort.PageContentView;
import com.logiccheck.document.port.DocumentStructurePort.SectionView;
import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.finding.EvidenceResponse;
import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingDraftRequest;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.finding.FindingResponse;
import com.logiccheck.review.finding.Verdict;

@Service
public class ReviewJobService {

    private static final int REPLY_DUE_DAYS = 7;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final BigDecimal MANUAL_CONFIDENCE = BigDecimal.ONE;

    private final ReviewJobRepository reviewJobRepository;
    private final FindingRepository findingRepository;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentStructurePort documentStructurePort;
    private final ReviewAiClient reviewAiClient;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;
    private final int parseDurationMs;
    private final int analyzeDurationMs;

    public ReviewJobService(ReviewJobRepository reviewJobRepository, FindingRepository findingRepository,
                            DocumentQueryPort documentQueryPort, DocumentStructurePort documentStructurePort,
                            ReviewAiClient reviewAiClient, AuthService authService,
                            ApplicationEventPublisher eventPublisher,
                            @Value("${review.parse-duration-ms}") int parseDurationMs,
                            @Value("${review.analyze-duration-ms}") int analyzeDurationMs) {
        this.reviewJobRepository = reviewJobRepository;
        this.findingRepository = findingRepository;
        this.documentQueryPort = documentQueryPort;
        this.documentStructurePort = documentStructurePort;
        this.reviewAiClient = reviewAiClient;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
        this.parseDurationMs = parseDurationMs;
        this.analyzeDurationMs = analyzeDurationMs;
    }

    /**
     * 분석을 접수하고 곧바로 돌려준다. 실제 처리는 커밋 후 백그라운드에서 이어진다.
     * 파싱과 분석은 수십 초가 걸릴 수 있어 요청을 붙잡고 있으면 타임아웃이 난다.
     */
    @Transactional
    public ReviewJobResponse create(Long ownerId, Long documentId) {
        ownedDocument(documentId, ownerId);
        if (reviewJobRepository.existsByDocumentIdAndStatusIn(documentId,
                List.of(JobStatus.PENDING, JobStatus.RUNNING))) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_RUNNING);
        }

        ReviewJob job;
        try {
            // 동시에 두 번 눌러도 하나만 남도록 DB의 부분 유니크 제약을 최종 방어선으로 둔다.
            job = reviewJobRepository.saveAndFlush(
                    ReviewJob.start(documentId, parseDurationMs, analyzeDurationMs, ReviewJobSteps.waiting()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_RUNNING);
        }

        eventPublisher.publishEvent(new ReviewJobStartedEvent(job.getId(), documentId, ownerId, parseDurationMs,
                analyzeDurationMs));
        return ReviewJobResponse.of(job, List.of(), JobSummary.of(List.of()));
    }

    @Transactional(readOnly = true)
    public ReviewJobResponse get(Long ownerId, Long jobId) {
        ReviewJob job = ownedJob(ownerId, jobId);
        return describe(job);
    }

    @Transactional(readOnly = true)
    public ReviewJobResponse latestForDocument(Long ownerId, Long documentId) {
        ownedDocument(documentId, ownerId);
        ReviewJob job = reviewJobRepository.findFirstByDocumentIdOrderByIdDesc(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_REVIEW_JOB));
        return describe(job);
    }

    @Transactional(readOnly = true)
    public List<OutlineResponse> sections(Long ownerId, Long jobId) {
        ReviewJob job = ownedJob(ownerId, jobId);
        Map<Long, Long> countBySection = findingRepository.findByJobIdOrderByOrderNoAsc(jobId).stream()
                .filter(f -> f.getSectionId() != null)
                .collect(Collectors.groupingBy(Finding::getSectionId, Collectors.counting()));

        return documentStructurePort.findSections(job.getDocumentId()).stream()
                .map(section -> toOutline(section, countBySection.getOrDefault(section.id(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentPageResponse> pages(Long ownerId, Long jobId) {
        ReviewJob job = ownedJob(ownerId, jobId);
        return documentStructurePort.findPageContents(job.getDocumentId()).stream()
                .map(this::toPage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FindingResponse> findings(Long ownerId, Long jobId) {
        ownedJob(ownerId, jobId);
        return findingRepository.findByJobIdOrderByOrderNoAsc(jobId).stream()
                .map(FindingResponse::from)
                .toList();
    }

    /** 원문에서 직접 잡아 만든 항목, 또는 질문 답변을 항목으로 올린 것. */
    @Transactional
    public FindingResponse addFinding(Long ownerId, Long jobId, FindingDraftRequest request) {
        ownedJob(ownerId, jobId);
        if (request.evidence() == null || request.evidence().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "evidence"));
        }

        int page = request.page() == null ? request.evidence().get(0).page() : request.page();
        BigDecimal confidence = request.confidence() == null
                ? MANUAL_CONFIDENCE
                : BigDecimal.valueOf(request.confidence());

        // 사람이 직접 올린 항목에는 검산 근거가 없다.
        Finding finding = Finding.of(jobId, request.findingType(), request.findingMethod(), request.sectionIdAsLong(),
                page, request.title(), request.description(), confidence, null, null, null,
                findingRepository.countByJobId(jobId));
        for (FindingDraftRequest.Evidence evidence : request.evidence()) {
            finding.addEvidence(evidence.anchorId(), evidence.page() == null ? page : evidence.page(),
                    evidence.label(), evidence.selectedText());
        }
        return FindingResponse.from(findingRepository.save(finding));
    }

    /** 판정. 미판정으로 되돌리는 것도 같은 경로로 처리한다. */
    @Transactional
    public FindingResponse decide(Long ownerId, Long jobId, Long findingId, Verdict verdict) {
        ownedJob(ownerId, jobId);
        Finding finding = findingRepository.findByIdAndJobId(findingId, jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        finding.decide(verdict);
        return FindingResponse.from(finding);
    }

    @Transactional
    public ReviewJobResponse complete(Long ownerId, Long jobId) {
        ReviewJob job = ownedJob(ownerId, jobId);
        if (job.isCompleted()) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_COMPLETED);
        }
        job.completeReview();
        return describe(job);
    }

    @Transactional(readOnly = true)
    public ReportResponse report(Long ownerId, Long jobId) {
        ReviewJob job = ownedJob(ownerId, jobId);
        DocumentMetaView document = ownedDocument(job.getDocumentId(), ownerId);
        List<Finding> all = findingRepository.findByJobIdOrderByOrderNoAsc(jobId);
        List<Finding> accepted = all.stream().filter(f -> f.getVerdict() == Verdict.ACCEPTED).toList();

        List<ReportResponse.ReportItem> items = java.util.stream.IntStream.range(0, accepted.size())
                .mapToObj(i -> new ReportResponse.ReportItem(i + 1, accepted.get(i).getFindingType().name(),
                        accepted.get(i).getPageNo(), accepted.get(i).getTitle()))
                .toList();

        return new ReportResponse(
                String.valueOf(job.getId()),
                String.valueOf(document.documentId()),
                document.title(),
                null,
                authService.profile(ownerId).email(),
                OffsetDateTime.now(KST),
                accepted.isEmpty() ? "PASS" : "CONDITIONAL",
                LocalDate.now(KST).plusDays(REPLY_DUE_DAYS).toString(),
                "",
                JobSummary.of(all),
                items,
                accepted.stream().map(f -> f.getTitle() + " 보완 자료 첨부").toList());
    }

    @Transactional(readOnly = true)
    public AnswerResponse ask(Long ownerId, Long jobId, QuestionRequest request) {
        ReviewJob job = ownedJob(ownerId, jobId);
        List<Finding> known = findingRepository.findByJobIdOrderByOrderNoAsc(jobId);

        ReviewAiAnswer answer = reviewAiClient.ask(new ReviewAiQuestion(
                request.question(),
                request.selectionText(),
                List.of(),
                known.stream().map(this::toKnownFinding).toList()));

        return toAnswerResponse(answer, job);
    }

    /* ------------------------------------------------------------------ */

    private ReviewJobResponse describe(ReviewJob job) {
        List<Finding> found = findingRepository.findByJobIdOrderByOrderNoAsc(job.getId());
        return ReviewJobResponse.of(job, found.stream().map(FindingResponse::from).toList(), JobSummary.of(found));
    }

    private ReviewJob ownedJob(Long ownerId, Long jobId) {
        ReviewJob job = reviewJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        ownedDocument(job.getDocumentId(), ownerId);
        return job;
    }

    private DocumentMetaView ownedDocument(Long documentId, Long ownerId) {
        return documentQueryPort.findMetaForOwner(documentId, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private OutlineResponse toOutline(SectionView section, long findingCount) {
        return new OutlineResponse(String.valueOf(section.id()), section.title(), section.level(), section.pageFrom(),
                findingCount);
    }

    private DocumentPageResponse toPage(PageContentView page) {
        return new DocumentPageResponse(page.pageNo(),
                page.sectionId() == null ? null : String.valueOf(page.sectionId()),
                page.sectionTitle(), page.blocks());
    }

    private ReviewAiQuestion.KnownFinding toKnownFinding(Finding finding) {
        List<ReviewAiResult.AiEvidence> evidence = finding.getEvidence().stream()
                .map(e -> new ReviewAiResult.AiEvidence(e.getAnchorId(), e.getPageNo(), e.getLabel()))
                .toList();
        return new ReviewAiQuestion.KnownFinding(finding.getFindingType().name(), finding.getPageNo(),
                finding.getTitle(), finding.getDescription(), evidence);
    }

    private AnswerResponse toAnswerResponse(ReviewAiAnswer answer, ReviewJob job) {
        List<EvidenceResponse> evidences = answer.evidences().stream()
                .map(e -> new EvidenceResponse(e.anchorId(), e.page(), e.label(), null))
                .toList();
        if (!answer.promotable() || answer.findingDraft() == null) {
            return new AnswerResponse(answer.answer(), evidences, false, null);
        }
        ReviewAiResult.AiFinding draft = answer.findingDraft();
        return new AnswerResponse(answer.answer(), evidences, true,
                new AnswerResponse.FindingDraft(draft.type(), draft.method(), draft.page(),
                        draft.sectionId() == null ? null : String.valueOf(draft.sectionId()), draft.title(),
                        draft.description(), draft.confidence(), evidences));
    }
}
