package com.logiccheck.review.finding;

import com.logiccheck.review.job.JobFindingSummary;
import com.logiccheck.review.job.dto.JobSummaryView;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * summary 집계. decided = accepted + rejected, open = status = OPEN 개수 (DEV3 D-3).
 * bySeverity 는 개수가 0 인 심각도도 키를 유지한다.
 */
@Component
public class FindingSummaryProvider implements JobFindingSummary {

    private final FindingRepository findingRepository;

    public FindingSummaryProvider(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public JobSummaryView summarize(Long jobId) {
        List<SeverityStatusCount> rows = findingRepository.countBySeverityAndStatus(jobId);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity severity : Severity.values()) {
            bySeverity.put(severity.name(), 0L);
        }

        long total = 0;
        long accepted = 0;
        long rejected = 0;
        long open = 0;

        for (SeverityStatusCount row : rows) {
            long count = row.count();
            total += count;
            bySeverity.merge(row.severity().name(), count, Long::sum);
            switch (row.status()) {
                case ACCEPTED -> accepted += count;
                case REJECTED -> rejected += count;
                case OPEN -> open += count;
            }
        }

        return new JobSummaryView(total, bySeverity, accepted + rejected, accepted, rejected, open);
    }
}
