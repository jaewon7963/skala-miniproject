// TEMP: 개발자2(document/port/) 구현 머지 시 이 스텁을 삭제하고 stub 프로파일을 뺀다. (DEV3 A-7 / E-2)
package com.logiccheck.review.support;

import com.logiccheck.document.port.DocumentStructurePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 기본은 빈 리스트다 (DEV3 E-2).
 *
 * review.stub.sample-structure=true 로 켜면 파이프라인을 끝까지 돌릴 수 있는 표본을 돌려준다.
 * 표본의 rawText 는 같은 페이지 textLayer 안에 그대로 들어 있어 인용문 검증(D-5)도 통과한다.
 */
@Primary
@Profile("stub")
@Component
public class StubDocumentStructurePort implements DocumentStructurePort {

    private static final String PAGE_9_TEXT =
            "3. 시장 진입 계획\n2027년 예상 매출 18억 원 을 전제로 3개 지역 40개 매장을 확보한다.";
    private static final String PAGE_11_TEXT =
            "5. 매출 · 재무 계획\n2027년 예상 매출 24억 원, 영업이익률 18% 를 목표로 한다.";

    private final boolean sampleStructure;

    public StubDocumentStructurePort(@Value("${review.stub.sample-structure:false}") boolean sampleStructure) {
        this.sampleStructure = sampleStructure;
    }

    @Override
    public List<PageView> findPages(Long documentId) {
        if (!sampleStructure) {
            return List.of();
        }
        return List.of(
                new PageView(9, 595.0, 842.0, PAGE_9_TEXT),
                new PageView(11, 595.0, 842.0, PAGE_11_TEXT)
        );
    }

    @Override
    public List<SectionView> findSections(Long documentId) {
        if (!sampleStructure) {
            return List.of();
        }
        return List.of(
                new SectionView(3L, null, "시장 진입 계획", 9, 3, "ORIGINAL"),
                new SectionView(5L, null, "매출 · 재무 계획", 11, 5, "ORIGINAL")
        );
    }

    @Override
    public List<ElementView> findElements(Long documentId) {
        if (!sampleStructure) {
            return List.of();
        }
        return List.of(
                new ElementView(901L, 9, "NUMBER", "2027년 예상 매출 18억 원",
                        new BigDecimal("18"), "억 원", new BBox(0.12, 0.42, 0.60, 0.03)),
                new ElementView(902L, 11, "NUMBER", "2027년 예상 매출 24억 원",
                        new BigDecimal("24"), "억 원", new BBox(0.12, 0.31, 0.66, 0.03)),
                // 다른 해의 수치는 같은 항목이 아니므로 지적 대상이 아니다
                new ElementView(903L, 11, "NUMBER", "2026년 예상 매출 9.6억 원",
                        new BigDecimal("9.6"), "억 원", new BBox(0.12, 0.36, 0.66, 0.03))
        );
    }
}
