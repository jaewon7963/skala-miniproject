package com.logiccheck.review.finding;

import com.logiccheck.global.exception.ErrorCode;
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
    private final ReviewJobService reviewJobService;

    public FindingService(FindingRepository findingRepository, ReviewJobService reviewJobService) {
        this.findingRepository = findingRepository;
        this.reviewJobService = reviewJobService;
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
        Finding finding = findingRepository.findWithEvidenceById(findingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        reviewJobService.findForOwner(finding.getJob().getId(), userId);
        return finding;
    }
}
