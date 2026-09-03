package com.logiccheck.review.finding;

import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

/** 엔티티에 세터가 없으므로 테스트에서만 리플렉션으로 값을 채운다. */
final class FindingFixtures {

    private FindingFixtures() {
    }

    static ReviewJob job(long id, JobStatus status) {
        ReviewJob job = ReviewJob.pending(1L);
        ReflectionTestUtils.setField(job, "id", id);
        ReflectionTestUtils.setField(job, "status", status);
        return job;
    }

    static Finding finding(long id, ReviewJob job, Severity severity, String confidence, Long ruleId) {
        Finding finding = new Finding() {
        };
        Finding target = finding;
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "job", job);
        ReflectionTestUtils.setField(target, "severity", severity);
        ReflectionTestUtils.setField(target, "status", FindingStatus.OPEN);
        ReflectionTestUtils.setField(target, "title", "제목 " + id);
        ReflectionTestUtils.setField(target, "confidence", confidence == null ? null : new BigDecimal(confidence));
        ReflectionTestUtils.setField(target, "pageNo", 11);
        ReflectionTestUtils.setField(target, "ruleId", ruleId);
        ReflectionTestUtils.setField(target, "evidence", List.of(evidence(id, target)));
        return target;
    }

    static Finding deterministic(long id, ReviewJob job, Severity severity, String confidence) {
        Finding finding = finding(id, job, severity, confidence, 7L);
        ReflectionTestUtils.setField(finding, "calcExpression", "3.2 + 9.6 + 24");
        ReflectionTestUtils.setField(finding, "calcExpected", "36.8억");
        ReflectionTestUtils.setField(finding, "calcActual", "24억");
        ReflectionTestUtils.setField(finding, "calcDiff", "3,200만 원");
        return finding;
    }

    static FindingEvidence evidence(long id, Finding finding) {
        FindingEvidence evidence = new FindingEvidence() {
        };
        ReflectionTestUtils.setField(evidence, "id", id * 10);
        ReflectionTestUtils.setField(evidence, "finding", finding);
        ReflectionTestUtils.setField(evidence, "pageNo", 11);
        ReflectionTestUtils.setField(evidence, "quote", "2027년 매출 24억 원");
        ReflectionTestUtils.setField(evidence, "label", "본문 문단 · p.11");
        ReflectionTestUtils.setField(evidence, "bboxX", new BigDecimal("0.120000"));
        ReflectionTestUtils.setField(evidence, "bboxY", new BigDecimal("0.310000"));
        ReflectionTestUtils.setField(evidence, "bboxW", new BigDecimal("0.660000"));
        ReflectionTestUtils.setField(evidence, "bboxH", new BigDecimal("0.030000"));
        ReflectionTestUtils.setField(evidence, "ordering", 0);
        return evidence;
    }
}
