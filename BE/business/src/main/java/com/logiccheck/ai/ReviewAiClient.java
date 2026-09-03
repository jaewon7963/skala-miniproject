package com.logiccheck.ai;

/** AI 관계 판단 (DEV3 D-10 5단계). */
public interface ReviewAiClient {

    ReviewAiResponse review(ReviewAiRequest request);
}
