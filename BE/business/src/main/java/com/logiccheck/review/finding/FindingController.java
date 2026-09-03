package com.logiccheck.review.finding;

import com.logiccheck.review.finding.dto.CreateDecisionRequest;
import com.logiccheck.review.finding.dto.FindingResponse;
import com.logiccheck.review.support.CurrentUser;
import com.logiccheck.review.support.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 명세 21 · 22 · 23. */
@RestController
@RequestMapping("/api")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    /** 21. 검토사항 목록 — Query 파라미터가 없다. 전체를 반환한다. */
    @GetMapping("/review-jobs/{jobId}/findings")
    public List<FindingResponse> list(@PathVariable Long jobId, @CurrentUser UserPrincipal user) {
        return findingService.findAllOfJob(jobId, user.userId()).stream()
                .map(FindingResponse::of)
                .toList();
    }

    /** 22. 검토사항 상세. */
    @GetMapping("/findings/{findingId}")
    public FindingResponse detail(@PathVariable Long findingId, @CurrentUser UserPrincipal user) {
        return FindingResponse.of(findingService.findOne(findingId, user.userId()));
    }

    /**
     * 23. 검토사항 판정. 갱신된 Finding 을 반환한다.
     * 판정 이력 조회 API 는 보류이므로 Location 헤더를 붙이지 않는다.
     */
    @PostMapping("/findings/{findingId}/decisions")
    public ResponseEntity<FindingResponse> decide(@PathVariable Long findingId,
                                                  @Valid @RequestBody CreateDecisionRequest request,
                                                  @CurrentUser UserPrincipal user) {
        Finding decided = findingService.decide(findingId, user.userId(), request.action(),
                request.note(), request.annotationBody());
        return ResponseEntity.status(HttpStatus.CREATED).body(FindingResponse.of(decided));
    }
}
