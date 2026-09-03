package com.logiccheck.review.job;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingType;
import com.logiccheck.review.finding.Verdict;

/**
 * 검토 진행 상황 요약.
 *
 * <p>{@code byType}에는 유형 세 가지 키가 항상 들어간다. 화면이 값을 직접 꺼내 쓰기 때문에
 * 개수가 0이라고 키를 빼면 그 자리에서 화면이 멈춘다.
 */
public record JobSummary(int total, Map<String, Integer> byType, int decided, int accepted, int rejected) {

    public static JobSummary of(List<Finding> findings) {
        Map<String, Integer> byType = java.util.Arrays.stream(FindingType.values())
                .collect(Collectors.toMap(Enum::name, type -> 0, (a, b) -> a, java.util.LinkedHashMap::new));
        findings.forEach(f -> byType.merge(f.getFindingType().name(), 1, Integer::sum));

        int accepted = (int) findings.stream().filter(f -> f.getVerdict() == Verdict.ACCEPTED).count();
        int rejected = (int) findings.stream().filter(f -> f.getVerdict() == Verdict.REJECTED).count();
        return new JobSummary(findings.size(), byType, accepted + rejected, accepted, rejected);
    }
}
