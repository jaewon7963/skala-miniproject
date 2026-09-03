package com.logiccheck.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.logiccheck.review.finding.Calculation;

/**
 * 검산 근거는 숫자로 저장하고 표기는 응답에서 만든다.
 * 예전에는 "36.8억" 같은 문자열을 그대로 저장해 숫자로 비교할 수 없었다.
 */
class CalculationTest {

    @Test
    void buildsKoreanNotationAndTheDifferenceFromStoredNumbers() {
        Calculation calculation = Calculation.of("3.2억 + 9.6억 + 24억",
                BigDecimal.valueOf(36_8000_0000L), BigDecimal.valueOf(24_0000_0000L));

        assertThat(calculation.expression()).isEqualTo("3.2억 + 9.6억 + 24억");
        assertThat(calculation.expected()).isEqualTo("36.8억");
        assertThat(calculation.actual()).isEqualTo("24억");
        assertThat(calculation.diff()).isEqualTo("12.8억");
    }

    @Test
    void hasNoDifferenceWhenOneSideIsMissing() {
        Calculation calculation = Calculation.of("합계", BigDecimal.valueOf(1000), null);

        assertThat(calculation.diff()).isNull();
        assertThat(calculation.actual()).isNull();
    }

    @Test
    void isAbsentWhenTheFindingHasNoCalculation() {
        assertThat(Calculation.of(null, null, null)).isNull();
    }
}
