package com.logiccheck.document.port;

import java.util.Optional;

/**
 * DEV3 A-5 Port ① — 개발자2 정의 · 개발자3 소비.
 * 분석 착수 검증(명세 16)과 Job 조회 응답 조합(명세 17)에 사용한다.
 * 시그니처는 확정이며 임의 변경 금지.
 */
public interface DocumentQueryPort {

    /** 소유자가 아니거나 soft delete 상태면 Optional.empty() */
    Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId);

    record DocumentMetaView(
            Long documentId,
            Long ownerId,
            String title,
            Integer pageCount,   // 파싱 전 null
            String parseStatus   // PENDING·PARSING·EXTRACTING·DONE·FAILED
    ) {}
}
