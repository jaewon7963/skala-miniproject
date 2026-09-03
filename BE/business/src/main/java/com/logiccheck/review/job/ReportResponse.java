package com.logiccheck.review.job;

import java.time.OffsetDateTime;
import java.util.List;

/** 검토 의견서 화면이 인쇄하는 데이터. 항목과 체크리스트는 검토 반영된 것만 담는다. */
public record ReportResponse(
        String jobId,
        String documentId,
        String documentName,
        Integer documentVersion,
        String reviewer,
        OffsetDateTime reviewedAt,
        String verdict,
        String dueDate,
        String receiver,
        JobSummary summary,
        List<ReportItem> items,
        List<String> checklist) {

    public record ReportItem(int no, String type, int page, String instruction) {
    }
}
