package com.logiccheck.review.job;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.logiccheck.review.finding.FindingResponse;

/**
 * 진행 화면과 검토 화면이 함께 쓰는 작업 표현.
 *
 * <p>진행 화면은 이 응답을 짧은 간격으로 반복해서 받아 진행률과 단계를 갱신하고,
 * {@code status}가 끝난 값이 될 때까지 멈추지 않는다. 그래서 어떤 경로로 끝나든
 * 작업은 반드시 DONE 아니면 FAILED에 도달해야 한다.
 */
public record ReviewJobResponse(
        String id,
        String documentId,
        String phase,
        String status,
        int parseProgress,
        int analyzeProgress,
        List<JobStep> steps,
        List<PartialFailure> partialFailures,
        List<FindingResponse> discovered,
        JobSummary summary,
        OffsetDateTime completedAt) {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static ReviewJobResponse of(ReviewJob job, List<FindingResponse> discovered, JobSummary summary) {
        return new ReviewJobResponse(
                String.valueOf(job.getId()),
                String.valueOf(job.getDocumentId()),
                job.getPhase().name(),
                job.getStatus().name(),
                job.parseProgress(),
                job.analyzeProgress(),
                ReviewJobSteps.describe(job.getStepStates()),
                job.getPartialFailures(),
                discovered,
                summary,
                job.getCompletedAt() == null ? null : job.getCompletedAt().atOffset(KST));
    }
}
