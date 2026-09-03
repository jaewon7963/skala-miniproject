// TEMP: AI 서버 엔드포인트를 확보하면 이 스텁을 삭제하고 review.ai.base-url 을 설정한다.
package com.logiccheck.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * AI 서버 없이 RAG 경로를 검증하기 위한 스텁.
 *
 * 첫 번째 요소의 rawText 를 그대로 인용문으로 쓴다 — 원문 대조를 통과해야 저장되는 것을 보이기 위함이다.
 * 마지막에는 일부러 원문에 없는 인용문을 하나 섞어 폐기 로직이 동작하는지 드러낸다.
 */
@Primary
@Profile("stub")
@Component
public class StubReviewAiClient implements ReviewAiClient {

    @Override
    public ReviewAiResponse review(ReviewAiRequest request) {
        List<ReviewAiRequest.Element> elements = request.elements();
        if (elements.isEmpty()) {
            return new ReviewAiResponse("stub-0.1", request.promptVersion(),
                    OffsetDateTime.now().toString(), List.of());
        }

        ReviewAiRequest.Element first = elements.get(0);
        return new ReviewAiResponse("stub-0.1", request.promptVersion(),
                OffsetDateTime.now().toString(),
                List.of(
                        // 원문에 있는 인용문 — 저장된다
                        new ReviewAiResponse.Finding("WARNING", first.sectionId(), first.page(),
                                "근거 문서와 목표치의 연결이 확인되지 않습니다",
                                "제시된 수치를 뒷받침하는 산출 근거가 문서 안에 없습니다.",
                                new BigDecimal("0.780"),
                                List.of(new ReviewAiResponse.Evidence(first.id(), first.page(),
                                        first.text(), "p." + first.page()))),
                        // 원문에 없는 인용문 — 폐기된다 (DEV3 D-5)
                        new ReviewAiResponse.Finding("INFO", null, first.page(),
                                "폐기되어야 하는 항목",
                                "원문에 존재하지 않는 인용문을 담고 있습니다.",
                                new BigDecimal("0.500"),
                                List.of(new ReviewAiResponse.Evidence(null, first.page(),
                                        "이 문장은 원문 어디에도 없습니다", null)))
                ));
    }
}
