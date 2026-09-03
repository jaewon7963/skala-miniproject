package com.logiccheck.review.annotation;

import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.annotation.Annotation.Anchor;
import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingEvidence;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.review.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final FindingRepository findingRepository;
    private final ReviewJobService reviewJobService;

    public AnnotationService(AnnotationRepository annotationRepository,
                             FindingRepository findingRepository,
                             ReviewJobService reviewJobService) {
        this.annotationRepository = annotationRepository;
        this.findingRepository = findingRepository;
        this.reviewJobService = reviewJobService;
    }

    /** 명세 24. 검토 화면 진입 시 1회 조회한다. 필터 없음. */
    @Transactional(readOnly = true)
    public List<Annotation> findAllOfJob(Long jobId, Long userId) {
        reviewJobService.findForOwner(jobId, userId);
        return annotationRepository.findByJobIdAndDeletedAtIsNullOrderByIdAsc(jobId);
    }

    /** 명세 25. */
    @Transactional
    public Annotation create(Long jobId, Long userId, String findingId, String body, Anchor anchor) {
        ReviewJob job = reviewJobService.findForOwner(jobId, userId).job();
        Finding finding = findingId == null ? null : requireFindingOfJob(findingId, jobId);

        // Finding 주석이면서 좌표를 안 보냈으면 해당 Finding 의 첫 evidence 를 복사한다.
        Anchor resolved = anchor != null ? anchor : anchorFromFirstEvidence(finding);

        return annotationRepository.save(Annotation.create(job, finding, userId, body, resolved));
    }

    /** 명세 26. 본문만 수정한다. */
    @Transactional
    public Annotation updateBody(Long annotationId, Long userId, String body) {
        Annotation annotation = requireOwnedAnnotation(annotationId, userId);
        annotation.changeBody(body);
        return annotation;
    }

    /** 명세 27. soft delete 후 목록에서 제외된다. */
    @Transactional
    public void delete(Long annotationId, Long userId) {
        requireOwnedAnnotation(annotationId, userId).delete();
    }

    /**
     * 판정(명세 23)에서 annotationBody 를 함께 받았을 때 같은 트랜잭션에서 호출한다 (미결 #5).
     * 소유권은 호출부가 이미 확인했다.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Annotation createForFinding(Finding finding, Long authorId, String body) {
        return annotationRepository.save(Annotation.create(
                finding.getJob(), finding, authorId, body, anchorFromFirstEvidence(finding)));
    }

    private Annotation requireOwnedAnnotation(Long annotationId, Long userId) {
        Annotation annotation = annotationRepository.findByIdAndDeletedAtIsNull(annotationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        reviewJobService.findForOwner(annotation.getJob().getId(), userId);
        return annotation;
    }

    private Finding requireFindingOfJob(String rawFindingId, Long jobId) {
        Long findingId;
        try {
            findingId = Long.valueOf(rawFindingId.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, java.util.Map.of("field", "findingId"));
        }
        Finding finding = findingRepository.findWithEvidenceById(findingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!finding.getJob().getId().equals(jobId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return finding;
    }

    private static Anchor anchorFromFirstEvidence(Finding finding) {
        if (finding == null || finding.getEvidence().isEmpty()) {
            return null;
        }
        FindingEvidence first = finding.getEvidence().get(0);
        return new Anchor(first.getPageNo(), first.getQuote(),
                first.getBboxX(), first.getBboxY(), first.getBboxW(), first.getBboxH());
    }
}
