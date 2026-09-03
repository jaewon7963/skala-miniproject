package com.logiccheck.review.finding;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logiccheck.ai.NumberText;

/**
 * 계산으로 찾아낸 항목의 근거식. 응답에만 쓰는 표현이다.
 *
 * <p>저장은 식·기댓값·실제값 세 컬럼으로 나눠서 한다. 예전에는 이 네 값을 JSON 하나에
 * 넣었는데, 값이 "36.8억" 같은 표시용 문자열이라 숫자로 비교하거나 집계할 수 없었다.
 * 지금은 숫자로 저장하고 사람이 읽는 표기는 여기서 만든다. 차이는 뺄셈이라 저장하지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Calculation(String expression, String expected, String actual, String diff) {

    /** 세 컬럼이 다 비어 있으면 계산 근거가 없는 항목이다 — null 을 돌려준다. */
    public static Calculation of(String expression, BigDecimal expected, BigDecimal actual) {
        if (expression == null && expected == null && actual == null) {
            return null;
        }
        String diff = expected == null || actual == null
                ? null
                : NumberText.format(expected.subtract(actual).abs().doubleValue());
        return new Calculation(expression, format(expected), format(actual), diff);
    }

    private static String format(BigDecimal value) {
        return value == null ? null : NumberText.format(value.doubleValue());
    }
}
