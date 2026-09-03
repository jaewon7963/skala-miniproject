package com.logiccheck.document.port;

import java.util.Collection;
import java.util.Map;

// ponytail: 실제 소유자는 review 도메인(com.logiccheck.review.port)이다. 그 도메인이
// 아직 없어 document 쪽에서 계약만 정의해 두었다 — review 도메인이 생기면 시그니처를
// 바꾸지 말고 파일을 그대로 옮긴 뒤 StubReviewJobQueryPort만 삭제한다.
public interface ReviewJobQueryPort {

    Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds);

    record LatestJobView(Long jobId, Long documentId, String status, String reviewStatus) {
    }
}
