package com.logiccheck.ai;

/**
 * 검토 분석과 질의응답을 담당하는 모델 창구.
 *
 * <p>지금은 규칙 기반 목업({@link StubReviewAiClient})이 붙어 있다. 실제 모델을 붙일 때는
 * 이 인터페이스를 구현하는 클래스를 하나 더 만들고 설정값만 바꾸면 되도록,
 * 입출력을 모델에 그대로 넘길 수 있는 모양으로 고정해 두었다.
 */
public interface ReviewAiClient {

    ReviewAiResult analyze(ReviewAiRequest request);

    ReviewAiAnswer ask(ReviewAiQuestion question);
}
