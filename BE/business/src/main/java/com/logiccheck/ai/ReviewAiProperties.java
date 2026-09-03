package com.logiccheck.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 서버 연동 설정 (DEV3 D-10 §8.10).
 *
 * - API Key 는 서버 측 보관, 환경변수로 분리한다. 코드·로그에 남기지 않는다.
 * - 모델명 · Temperature 등은 코드가 아닌 설정으로 분리한다.
 * - Timeout 을 반드시 설정한다.
 */
@ConfigurationProperties(prefix = "review.ai")
public record ReviewAiProperties(
        boolean enabled,
        String baseUrl,
        String path,
        String apiKey,
        String model,
        Double temperature,
        String promptVersion,
        Duration connectTimeout,
        Duration readTimeout
) {

    public ReviewAiProperties {
        path = path == null ? "/v1/review" : path;
        model = model == null ? "" : model;
        temperature = temperature == null ? 0.0 : temperature;
        promptVersion = promptVersion == null ? "review-v1" : promptVersion;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
    }

    /**
     * API Key 가 로그로 새지 않게 toString 을 직접 막는다.
     * record 기본 toString 은 모든 컴포넌트를 그대로 찍는다.
     */
    @Override
    public String toString() {
        return "ReviewAiProperties[enabled=%s, baseUrl=%s, path=%s, apiKey=%s, model=%s, temperature=%s, "
                .formatted(enabled, baseUrl, path, apiKey == null || apiKey.isBlank() ? "unset" : "***",
                        model, temperature)
                + "promptVersion=%s, connectTimeout=%s, readTimeout=%s]"
                .formatted(promptVersion, connectTimeout, readTimeout);
    }
}
