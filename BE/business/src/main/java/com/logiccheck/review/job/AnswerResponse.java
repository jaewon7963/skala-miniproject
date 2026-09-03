package com.logiccheck.review.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logiccheck.review.finding.EvidenceResponse;

/**
 * 질문에 대한 답.
 *
 * <p>근거 목록의 이름이 검토 항목 쪽({@code evidence})과 달리 복수형인 것은
 * 화면이 그렇게 읽기 때문이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnswerResponse(String answer, List<EvidenceResponse> evidences, boolean promotable,
                              FindingDraft findingDraft) {

    public record FindingDraft(String type, String method, int page, String sectionId, String title,
                                String description, double confidence, List<EvidenceResponse> evidence) {
    }
}
