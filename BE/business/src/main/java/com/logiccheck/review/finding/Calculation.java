package com.logiccheck.review.finding;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 계산으로 찾아낸 항목의 근거식. {@link FindingMethod#DETERMINISTIC} 일 때만 채운다.
 * 예) expression "3.2 + 9.6 + 24", expected "36.8억", actual "24억", diff "12.8억"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Calculation(String expression, String expected, String actual, String diff) {
}
