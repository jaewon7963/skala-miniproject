package com.logiccheck.document.port;

import java.math.BigDecimal;
import java.util.List;

import com.logiccheck.document.entity.PageBlock;

// Dev2(document 도메인) 소유, 향후 review 도메인이 소비하는 포트.
public interface DocumentStructurePort {

    List<PageView> findPages(Long documentId);

    List<SectionView> findSections(Long documentId);

    List<ElementView> findElements(Long documentId);

    /** 원문 뷰어와 분석이 함께 쓰는 페이지 내용. 텍스트를 제목·문단·표로 나눈 결과가 들어 있다. */
    List<PageContentView> findPageContents(Long documentId);

    record PageView(int pageNo, Double width, Double height, String textLayer) {
    }

    record SectionView(Long id, Long parentId, String title, int level, Integer pageFrom, Integer pageTo,
                        int orderNo, String source) {
    }

    // extracted_elements는 이번 MVP 파싱 범위에서 채우지 않는다(수치/표/주장/날짜
    // 추출은 스펙에 규칙이 없는 별도 기능). findElements는 항상 빈 리스트를 반환한다.
    record ElementView(Long id, int pageNo, String kind, String rawText, BigDecimal normalizedValue, String unit) {
    }

    record PageContentView(int pageNo, Long sectionId, String sectionTitle, List<PageBlock> blocks) {
    }
}
