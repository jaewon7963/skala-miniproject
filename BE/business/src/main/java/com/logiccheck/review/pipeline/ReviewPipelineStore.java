package com.logiccheck.review.pipeline;

import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingElement;
import com.logiccheck.review.finding.FindingElementRepository;
import com.logiccheck.review.finding.FindingEvidence;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 파이프라인의 트랜잭션 경계. 상태 전이와 저장을 짧은 트랜잭션으로 나눈다.
 * 오케스트레이션(ReviewPipeline)은 트랜잭션을 열지 않아 AI 호출이 커넥션을 붙잡지 않는다.
 */
@Component
public class ReviewPipelineStore {

    private final ReviewJobRepository reviewJobRepository;
    private final FindingRepository findingRepository;
    private final FindingElementRepository findingElementRepository;

    public ReviewPipelineStore(ReviewJobRepository reviewJobRepository,
                               FindingRepository findingRepository,
                               FindingElementRepository findingElementRepository) {
        this.reviewJobRepository = reviewJobRepository;
        this.findingRepository = findingRepository;
        this.findingElementRepository = findingElementRepository;
    }

    /** PENDING 인 Job 만 착수한다. 이미 다른 실행이 집어간 Job 이면 empty. */
    @Transactional
    public Optional<Long> startRunning(Long jobId, String rulesetVersion) {
        return reviewJobRepository.findById(jobId)
                .filter(job -> job.getStatus() == JobStatus.PENDING)
                .map(job -> {
                    job.markRunning(rulesetVersion);
                    return job.getDocumentId();
                });
    }

    /** 검토사항 · 근거 · 관련 요소를 한 트랜잭션에서 저장한다 (DEV3 D-10 6~8단계). */
    @Transactional
    public int saveFindings(Long jobId, List<FindingDraft> drafts) {
        ReviewJob job = reviewJobRepository.getReferenceById(jobId);
        int saved = 0;
        for (FindingDraft draft : drafts) {
            Finding finding = Finding.open(job, draft.ruleId(), draft.severity(), draft.title(),
                    draft.description(), draft.confidence(), draft.pageNo(), draft.sectionId());
            if (draft.calculation() != null) {
                finding.applyCalculation(draft.calculation().expression(), draft.calculation().expected(),
                        draft.calculation().actual(), draft.calculation().diff());
            }

            int ordering = 0;
            for (FindingDraft.EvidenceDraft evidence : draft.evidence()) {
                finding.addEvidence(FindingEvidence.of(finding, evidence.pageNo(), evidence.quote(),
                        evidence.label(), evidence.bboxX(), evidence.bboxY(), evidence.bboxW(),
                        evidence.bboxH(), evidence.charStart(), evidence.charEnd(), ordering++));
            }
            findingRepository.save(finding);

            if (draft.elementIds() != null) {
                for (Long elementId : draft.elementIds()) {
                    findingElementRepository.save(FindingElement.of(finding, elementId, null));
                }
            }
            saved++;
        }
        return saved;
    }

    @Transactional
    public void markDone(Long jobId) {
        reviewJobRepository.findById(jobId).ifPresent(ReviewJob::markDone);
    }

    @Transactional
    public void markFailed(Long jobId, String errorCode) {
        reviewJobRepository.findById(jobId).ifPresent(job -> job.markFailed(errorCode));
    }
}
