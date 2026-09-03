// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

import com.logiccheck.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 개발자1의 JWT 필터가 없는 동안 X-User-Id 헤더로 인증 주체를 대체한다.
 * stub 프로파일에서만 등록되므로 운영 경로에는 노출되지 않는다.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    static final String HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String raw = webRequest.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return new UserPrincipal(Long.valueOf(raw.trim()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
