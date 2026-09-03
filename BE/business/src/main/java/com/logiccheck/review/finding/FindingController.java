package com.logiccheck.review.finding;

import com.logiccheck.review.finding.dto.FindingResponse;
import com.logiccheck.review.support.CurrentUser;
import com.logiccheck.review.support.UserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 명세 21 · 22. */
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
}
