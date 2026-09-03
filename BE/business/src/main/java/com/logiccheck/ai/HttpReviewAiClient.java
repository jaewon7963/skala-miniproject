package com.logiccheck.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 서버 HTTP 클라이언트.
 *
 * API Key 는 설정(환경변수)에서 읽어 헤더로만 보낸다. 로그에 남기지 않는다 (DEV3 D-10 §8.10).
 * Timeout 은 review.ai.connect-timeout · read-timeout 으로 설정한다.
 *
 * stub 프로파일에서는 등록하지 않는다. StubReviewAiClient 가 대신 들어가므로
 * base-url 없이도 AI 경로를 돌릴 수 있다.
 */
@Component
@Profile("!stub")
@ConditionalOnProperty(prefix = "review.ai", name = "enabled", havingValue = "true")
public class HttpReviewAiClient implements ReviewAiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpReviewAiClient.class);

    private final ReviewAiProperties properties;
    private final RestClient restClient;

    public HttpReviewAiClient(ReviewAiProperties properties) {
        this.properties = properties;
        this.restClient = build(properties);
        log.info("AI 클라이언트를 초기화했다. {}", properties);
    }

    private static RestClient build(ReviewAiProperties properties) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException("review.ai.enabled=true 인데 review.ai.base-url 이 비어 있다.");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
        }
        return builder.build();
    }

    @Override
    public ReviewAiResponse review(ReviewAiRequest request) {
        try {
            ReviewAiResponse response = restClient.post()
                    .uri(properties.path())
                    .body(request)
                    .retrieve()
                    .body(ReviewAiResponse.class);
            if (response == null) {
                throw new ReviewAiException("AI_RESPONSE_INVALID", "AI 서버가 빈 본문을 반환했다.");
            }
            return response;
        } catch (ReviewAiException e) {
            throw e;
        } catch (RestClientException e) {
            // 예외 메시지에 요청 본문이나 헤더가 섞여 나가지 않게 메시지를 직접 만든다.
            throw new ReviewAiException("AI_CALL_FAILED",
                    "AI 서버 호출에 실패했다: " + e.getClass().getSimpleName(), e);
        }
    }
}
