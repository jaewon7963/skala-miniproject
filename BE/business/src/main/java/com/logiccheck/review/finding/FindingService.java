package com.logiccheck.review.finding;

import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.annotation.AnnotationService;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.review.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class FindingService {

    /**
     * 명세 21 의 정렬: severity → confidence DESC (DEV3 D-4).
     * severity 는 의미 순서(ERROR > WARNING > INFO)를 쓴다. confidence 가 null 이면 뒤로 보낸다.
     */
    private static final Comparator<Finding> LIST_ORDER = Comparator
            .comparingInt((Finding f) -> f.getSeverity().order())
            .thenComparing(Finding::getConfidence,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Finding::getId);

    private final FindingRepository findingRepository;
    private final FindingDecisionRepository findingDecisionRepository;
    private final ReviewJobService reviewJobService;
    private final AnnotationService annotationService;

    public FindingService(FindingRepository findingRepository,
                          FindingDecisionRepository findingDecisionRepository,
                          ReviewJobService reviewJobService,
                          AnnotationService annotationService) {
        this.findingRepository = findingRepository;
        this.findingDecisionRepository = findingDecisionRepository;
        this.reviewJobService = reviewJobService;
        this.annotationService = annotationService;
    }

    /**
     * 명세 21. Query 파라미터를 받지 않고 전체를 반환한다 — 정렬·필터는 FE 가 처리한다 (DEV3 D-4).
     * 분석 미완료 Job(PENDING · RUNNING)은 404 가 아니라 빈 배열이다.
     */
    @Transactional(readOnly = true)
    public List<Finding> findAllOfJob(Long jobId, Long userId) {
        ReviewJob job = reviewJobService.findForOwner(jobId, userId).job();
        if (!job.getStatus().isTerminal()) {
            return List.of();
        }
        return findingRepository.findByJobId(jobId).stream().sorted(LIST_ORDER).toList();
    }

    /** 명세 22. 타인의 Job 에 속한 Finding 은 403 이다. */
    @Transactional(readOnly = true)
    public Finding findOne(Long findingId, Long userId) {
        return requireOwnedFinding(findingId, userId);
    }

    /**
     * 명세 23. findings.status 를 갱신하고 finding_decisions 에 이력을 누적한다 (DEV3 D-6).
     *
     * review_status = COMPLETED 인 Job 의 Finding 은 판정을 변경할 수 없다 → 409.
     * 완료되지 않은 Job 이면 재판정은 허용하고 이력만 쌓인다.
     *
     * annotationBody 가 있으면 같은 트랜잭션에서 주석까지 저장한다 (미결 #5 통합 채택).
     * 주석 저장이 실패하면 판정도 함께 롤백된다.
     */
    @Transactional
    public Finding decide(Long findingId, Long userId, DecisionAction action,
                          String note, String annotationBody) {
        Finding finding = requireOwnedFinding(findingId, userId);
        if (finding.getJob().isReviewCompleted()) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_COMPLETED);
        }

        FindingStatus before = finding.getStatus();
        finding.decide(action);
        findingDecisionRepository.save(FindingDecision.of(finding, userId, action, before, note));

        if (annotationBody != null && !annotationBody.isBlank()) {
            annotationService.createForFinding(finding, userId, annotationBody);
        }
        return finding;
    }

    private Finding requireOwnedFinding(Long findingId, Long userId) {
        Finding finding = findingRepository.findWithEvidenceById(findingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        reviewJobService.findForOwner(finding.getJob().getId(), userId);
        return finding;
    }
}
