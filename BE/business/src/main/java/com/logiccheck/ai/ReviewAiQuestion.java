package com.logiccheck.ai;

import java.util.List;

/** 검토 화면의 질문 패널에서 올라온 질의. {@code selection}은 사용자가 원문에서 잡아 놓은 구절이다. */
public record ReviewAiQuestion(String question, String selection, List<ReviewAiRequest.AnalyzedPage> pages,
                                List<KnownFinding> knownFindings) {

    public record KnownFinding(String type, int page, String title, String description,
                                List<ReviewAiResult.AiEvidence> evidence) {
    }
}
