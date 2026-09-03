// TEMP: 개발자2(document/port/) 구현 머지 시 이 스텁을 삭제하고 stub 프로파일을 뺀다. (DEV3 A-7 / E-2)
package com.logiccheck.review.support;

import com.logiccheck.document.port.DocumentQueryPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * parseStatus = "DONE" 을 돌려주어 명세 16번의 선행 조건을 통과시킨다.
 * DOCUMENT_NOT_READY 경로를 확인할 때만 "PARSING" 으로 바꾼다.
 */
@Primary
@Profile("stub")
@Component
public class StubDocumentQueryPort implements DocumentQueryPort {

    @Override
    public Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId) {
        return Optional.of(new DocumentMetaView(documentId, userId, "스텁 문서", 21, "DONE"));
    }
}
