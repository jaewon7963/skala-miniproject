package com.logiccheck.review.job;

import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.ReviewJobService.JobWithDocument;
import com.logiccheck.review.job.dto.CreateReviewJobRequest;
import com.logiccheck.review.job.dto.ReviewJobResponse;
import com.logiccheck.review.support.BusinessException;
import com.logiccheck.review.support.CurrentUser;
import com.logiccheck.review.support.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * 명세 16 · 17 · 18 · 19.
 *
 * 18 번은 경로가 /api/documents/... 지만 리소스가 ReviewJob 이므로 개발자3 담당이다 (DEV3 B-3).
 * 개발자2의 DocumentController 와 같은 prefix 를 공유하므로 양쪽 다 와일드카드 매핑을 쓰지 않고
 * 정확한 경로만 명시한다.
 */
@RestController
@RequestMapping("/api")
public class ReviewJobController {

    private final ReviewJobService reviewJobService;

    public ReviewJobController(ReviewJobService reviewJobService) {
        this.reviewJobService = reviewJobService;
    }

    /** 16. 분석 시작 — 202 를 즉시 반환하고 파이프라인 완료를 기다리지 않는다. */
    @PostMapping("/review-jobs")
    public ResponseEntity<ReviewJobResponse> create(@Valid @RequestBody CreateReviewJobRequest request,
                                                    @CurrentUser UserPrincipal user) {
        Long documentId = parseId(request.documentId(), "documentId");
        ReviewJob job = reviewJobService.start(documentId, user.userId());

        JobWithDocument found = reviewJobService.findForOwner(job.getId(), user.userId());
        return ResponseEntity.accepted()
                .location(URI.create("/api/review-jobs/" + job.getId()))
                .body(ReviewJobResponse.of(found.job(), found.document(), found.summary()));
    }

    /** 17. 분석 작업 조회 — FE 폴링과 검토 화면 진입에 쓰인다. */
    @GetMapping("/review-jobs/{jobId}")
    public ReviewJobResponse get(@PathVariable Long jobId, @CurrentUser UserPrincipal user) {
        JobWithDocument found = reviewJobService.findForOwner(jobId, user.userId());
        return ReviewJobResponse.of(found.job(), found.document(), found.summary());
    }

    /** 18. 최근 분석 작업 조회. */
    @GetMapping("/documents/{documentId}/review-jobs/latest")
    public ReviewJobResponse latest(@PathVariable Long documentId, @CurrentUser UserPrincipal user) {
        JobWithDocument found = reviewJobService.findLatestForOwner(documentId, user.userId());
        return ReviewJobResponse.of(found.job(), found.document(), found.summary());
    }

    /** 19. 검토 완료 처리. */
    @PostMapping("/review-jobs/{jobId}/complete")
    public ReviewJobResponse complete(@PathVariable Long jobId, @CurrentUser UserPrincipal user) {
        JobWithDocument found = reviewJobService.completeReview(jobId, user.userId());
        return ReviewJobResponse.of(found.job(), found.document(), found.summary());
    }

    private static Long parseId(String raw, String field) {
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", field));
        }
    }
}
