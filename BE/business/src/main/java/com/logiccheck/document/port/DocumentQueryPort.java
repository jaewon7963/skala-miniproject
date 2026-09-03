package com.logiccheck.document.port;

import java.util.Optional;

// Dev2(document 도메인) 소유, 향후 review 도메인이 소비하는 포트.
public interface DocumentQueryPort {

    Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId);

    record DocumentMetaView(Long documentId, Long ownerId, String title, Integer pageCount, String parseStatus) {
    }
}
