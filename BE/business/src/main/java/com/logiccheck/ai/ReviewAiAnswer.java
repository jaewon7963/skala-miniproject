package com.logiccheck.ai;

import java.util.List;

/**
 * 질문에 대한 답.
 *
 * <p>근거를 찾지 못하면 추측해서 답하지 않고 {@code evidences}를 비운 채 못 찾았다고 말한다.
 * {@code promotable}이 참이면 화면이 이 답을 검토 항목으로 승격할 수 있다.
 */
public record ReviewAiAnswer(String answer, List<ReviewAiResult.AiEvidence> evidences, boolean promotable,
                              ReviewAiResult.AiFinding findingDraft) {

    public static ReviewAiAnswer notFound() {
        return new ReviewAiAnswer("이 문서 안에서 관련 근거를 찾지 못했습니다. 추측하지 않습니다.", List.of(), false, null);
    }
}
