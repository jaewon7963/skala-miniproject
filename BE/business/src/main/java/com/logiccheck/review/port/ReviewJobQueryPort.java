package com.logiccheck.review.port;

import java.util.Collection;
import java.util.Map;

/**
 * 문서 도메인이 목록의 상태 배지와 개수를 계산할 때 쓰는 조회 창구.
 *
 * <p>문서 여러 건을 한 번에 넘기는 시그니처인 이유는 목록 화면 때문이다.
 * 단건으로 바꾸면 문서 수만큼 쿼리가 나간다.
 */
public interface ReviewJobQueryPort {

    /** 문서별 최신 작업 1건. 작업이 없는 문서는 Map에 키가 없다. */
    Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds);

    record LatestJobView(Long jobId, Long documentId, String status, String reviewStatus) {
    }
}
