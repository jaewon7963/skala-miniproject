// TEMP: 개발자2(document/port/) 구현 머지 시 이 스텁을 삭제하고 stub 프로파일을 뺀다. (DEV3 A-7 / E-2)
package com.logiccheck.review.support;

import com.logiccheck.document.port.DocumentStructurePort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** AI 파이프라인(5단계)까지는 빈 리스트로 충분하다. (DEV3 E-2) */
@Primary
@Profile("stub")
@Component
public class StubDocumentStructurePort implements DocumentStructurePort {

    @Override
    public List<PageView> findPages(Long documentId) {
        return List.of();
    }

    @Override
    public List<SectionView> findSections(Long documentId) {
        return List.of();
    }

    @Override
    public List<ElementView> findElements(Long documentId) {
        return List.of();
    }
}
