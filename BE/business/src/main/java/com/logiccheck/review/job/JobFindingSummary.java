package com.logiccheck.review.job;

import com.logiccheck.review.job.dto.JobSummaryView;

/**
 * 명세 17 의 summary 를 review/finding 에서 가져오기 위한 경계.
 * job 이 finding 내부를 직접 알지 않게 인터페이스를 job 쪽에 둔다.
 */
public interface JobFindingSummary {

    JobSummaryView summarize(Long jobId);
}
