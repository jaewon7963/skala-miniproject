package com.logiccheck.review.job;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logiccheck.global.security.CurrentUser;
import com.logiccheck.review.finding.FindingDraftRequest;
import com.logiccheck.review.finding.FindingResponse;
import com.logiccheck.review.finding.VerdictRequest;

/**
 * 분석과 검토 화면이 쓰는 경로.
 *
 * <p>문서 아래에 걸린 경로가 하나 있지만({@code /api/documents/{id}/review-jobs/latest})
 * 돌려주는 것이 분석 작업이라 여기에 둔다. 문서 쪽 컨트롤러와 경로가 겹치지 않도록
 * 양쪽 모두 정확한 경로만 쓴다.
 */
@RestController
@RequestMapping("/api")
public class ReviewJobController {

    private final ReviewJobService reviewJobService;

    public ReviewJobController(ReviewJobService reviewJobService) {
        this.reviewJobService = reviewJobService;
    }

    /** 분석 접수. 처리를 기다리지 않고 바로 돌려준다. */
    @PostMapping("/review-jobs")
    public ResponseEntity<ReviewJobResponse> create(@RequestBody CreateReviewJobRequest request,
                                                     @CurrentUser Long ownerId) {
        ReviewJobResponse job = reviewJobService.create(ownerId, request.documentIdAsLong());
        return ResponseEntity.accepted()
                .location(URI.create("/api/review-jobs/" + job.id()))
                .body(job);
    }

    @GetMapping("/review-jobs/{jobId}")
    public ReviewJobResponse get(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.get(ownerId, jobId);
    }

    @GetMapping("/documents/{documentId}/review-jobs/latest")
    public ReviewJobResponse latest(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        return reviewJobService.latestForDocument(ownerId, documentId);
    }

    @GetMapping("/review-jobs/{jobId}/sections")
    public List<OutlineResponse> sections(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.sections(ownerId, jobId);
    }

    @GetMapping("/review-jobs/{jobId}/pages")
    public List<DocumentPageResponse> pages(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.pages(ownerId, jobId);
    }

    /** 검토 항목 전체를 한 번에 준다. 정렬과 필터는 화면이 처리한다. */
    @GetMapping("/review-jobs/{jobId}/findings")
    public List<FindingResponse> findings(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.findings(ownerId, jobId);
    }

    @PostMapping("/review-jobs/{jobId}/findings")
    public FindingResponse addFinding(@PathVariable Long jobId, @RequestBody FindingDraftRequest request,
                                       @CurrentUser Long ownerId) {
        return reviewJobService.addFinding(ownerId, jobId, request);
    }

    @PatchMapping("/review-jobs/{jobId}/findings/{findingId}/verdict")
    public FindingResponse decide(@PathVariable Long jobId, @PathVariable Long findingId,
                                   @RequestBody VerdictRequest request, @CurrentUser Long ownerId) {
        return reviewJobService.decide(ownerId, jobId, findingId, request.toVerdict());
    }

    @PostMapping("/review-jobs/{jobId}/complete")
    public ReviewJobResponse complete(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.complete(ownerId, jobId);
    }

    @GetMapping("/review-jobs/{jobId}/report")
    public ReportResponse report(@PathVariable Long jobId, @CurrentUser Long ownerId) {
        return reviewJobService.report(ownerId, jobId);
    }

    @PostMapping("/review-jobs/{jobId}/questions")
    public AnswerResponse ask(@PathVariable Long jobId, @RequestBody QuestionRequest request,
                               @CurrentUser Long ownerId) {
        return reviewJobService.ask(ownerId, jobId, request);
    }
}
