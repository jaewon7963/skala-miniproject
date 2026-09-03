package com.logiccheck.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문서에 적힌 한국식 수치를 비교 가능한 숫자로 바꾼다.
 *
 * <p>"3.2억", "2억 3,200만", "1,250", "18%" 처럼 단위와 자릿점이 섞인 표기를 다룬다.
 * 한 칸에 단위가 여러 번 나오면(2억 3,200만) 모두 더한다.
 */
final class NumberText {

    private static final Pattern UNIT_NUMBER = Pattern.compile("(-?[\\d,]+(?:\\.\\d+)?)\\s*(조|억|만|천)?");
    private static final Pattern COUNT_WITH_NOUN = Pattern.compile("([\\d,]+)\\s*개\\s*([가-힣]{2,6})");
    /** 조사가 붙으면 "매장"과 "매장을"이 다른 대상으로 갈리므로 끝의 조사를 떼고 비교한다. */
    private static final Pattern TRAILING_PARTICLE = Pattern.compile("(을|를|은|는|이|가|의|에|와|과|도|만|으로|로)$");

    private NumberText() {
    }

    /** 숫자로 읽히지 않으면 비어 있는 값을 돌려준다. 라벨 칸("구분", "합계")이 여기에 해당한다. */
    static Optional<Double> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = raw.trim();
        if (text.equals("-") || text.equals("—")) {
            return Optional.empty();
        }

        Matcher matcher = UNIT_NUMBER.matcher(text);
        double total = 0;
        boolean found = false;
        while (matcher.find()) {
            String digits = matcher.group(1).replace(",", "");
            if (digits.isEmpty() || digits.equals("-")) {
                continue;
            }
            total += Double.parseDouble(digits) * multiplier(matcher.group(2));
            found = true;
        }
        return found ? Optional.of(total) : Optional.empty();
    }

    private static double multiplier(String unit) {
        if (unit == null) {
            return 1;
        }
        return switch (unit) {
            case "조" -> 1_000_000_000_000d;
            case "억" -> 100_000_000d;
            case "만" -> 10_000d;
            case "천" -> 1_000d;
            default -> 1;
        };
    }

    /** "40개 매장", "62개 매장" 처럼 세는 대상과 개수를 함께 뽑는다. 문서 안에서 전제가 어긋난 곳을 찾는 데 쓴다. */
    static List<Count> counts(String text) {
        List<Count> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        Matcher matcher = COUNT_WITH_NOUN.matcher(text);
        while (matcher.find()) {
            result.add(new Count(stripParticle(matcher.group(2)),
                    Double.parseDouble(matcher.group(1).replace(",", ""))));
        }
        return result;
    }

    private static String stripParticle(String noun) {
        String stripped = TRAILING_PARTICLE.matcher(noun).replaceFirst("");
        return stripped.length() >= 2 ? stripped : noun;
    }

    /** 사람이 읽는 표기로 되돌린다. 검토 항목 설명에 그대로 들어간다. */
    static String format(double value) {
        double abs = Math.abs(value);
        if (abs >= 100_000_000d) {
            return trim(value / 100_000_000d) + "억";
        }
        if (abs >= 10_000d) {
            return trim(value / 10_000d) + "만";
        }
        return trim(value);
    }

    private static String trim(double value) {
        double rounded = Math.round(value * 100) / 100.0;
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }

    record Count(String noun, double value) {
    }
}
