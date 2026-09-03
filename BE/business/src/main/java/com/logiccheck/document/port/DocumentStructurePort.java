package com.logiccheck.document.port;

import java.math.BigDecimal;
import java.util.List;

/**
 * DEV3 A-5 Port ② — 개발자2 정의 · 개발자3 소비.
 * 분석 파이프라인(DEV3 D-10)의 입력이다. 시그니처는 확정이며 임의 변경 금지.
 */
public interface DocumentStructurePort {

    List<PageView> findPages(Long documentId);

    List<SectionView> findSections(Long documentId);

    List<ElementView> findElements(Long documentId);

    record PageView(int pageNo, Double width, Double height, String textLayer) {}

    record SectionView(Long id, Long parentId, String title, Integer pageNo,
                       int ordering, String source) {}          // source: ORIGINAL·EXTRACTED

    record ElementView(Long id, int pageNo, String kind, String rawText,
                       BigDecimal numericValue, String unit, BBox bbox) {}

    record BBox(double x, double y, double w, double h) {}      // 0~1 상대 좌표
}
