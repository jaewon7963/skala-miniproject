package com.logiccheck.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;

/**
 * Swagger UI 설정. 경로와 응답 모양은 springdoc 이 컨트롤러를 스캔해 알아서 만들므로
 * 여기서는 문서 머리말과 인증 방식만 알려 준다.
 *
 * <p>이 서비스는 {@code /api/health} 와 가입·로그인을 뺀 거의 모든 경로가 JWT 를 요구한다.
 * Authorize 버튼이 없으면 화면에서 눌러 봐야 전부 401 이라 시험을 할 수가 없어서,
 * 스킴을 전역 요구사항으로 걸어 둔다.
 *
 * <p>접속: <a href="http://localhost:8081/swagger-ui.html">http://localhost:8081/swagger-ui.html</a>
 * — nginx 는 {@code /api} 만 넘기므로 5173 이 아니라 백엔드 포트로 직접 들어가야 한다.
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "POST /api/auth/login 의 응답에 담긴 token 값을 그대로 넣는다. 'Bearer ' 는 붙이지 않는다.")
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI bizxrayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BizXray API")
                        .version("v1")
                        .description("""
                                사업계획서를 올리면 파싱·분석해 검토 항목을 뽑아 주는 서비스의 API.

                                시험해 보려면 먼저 `POST /api/auth/login` 을 아래 값으로 실행하고,
                                응답의 `token` 을 우측 상단 **Authorize** 에 넣는다.

                                ```json
                                { "email": "kim@company.com", "password": "logic1234" }
                                ```
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
