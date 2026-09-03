package com.logiccheck.review.port;

import java.util.Collection;
import java.util.Map;

/**
 * DEV3 A-5 Port ③ — 개발자3 정의 · 개발자2 소비.
 * 문서 목록(명세 7)의 displayStatus 와 counts 계산에 사용한다.
 * 시그니처는 확정이며 임의 변경 금지.
 *
 * 복수 조회 시그니처를 단건으로 바꾸면 개발자2 쪽에서 N+1 이 발생한다 (DEV3 E-1).
 */
public interface ReviewJobQueryPort {

    /** 문서별 최신 Job 1건. Job 이 없는 문서는 Map 에 키가 없다 */
    Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds);

    record LatestJobView(
            Long jobId,
            Long documentId,
            String status,        // PENDING·RUNNING·DONE·FAILED
            String reviewStatus   // IN_REVIEW·COMPLETED
    ) {}
}
