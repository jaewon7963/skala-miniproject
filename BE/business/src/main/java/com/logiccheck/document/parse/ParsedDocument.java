package com.logiccheck.document.parse;

import java.math.BigDecimal;
import java.util.List;

public record ParsedDocument(int pageCount, List<ParsedPage> pages, List<ParsedSection> sections) {

    public record ParsedPage(int pageNo, BigDecimal width, BigDecimal height, String text) {
    }

    // pageTo는 이번 파싱 범위에서 계산하지 않는다(다음 형제 섹션의 시작 페이지를
    // 알아야 하는 계산이라 범위 밖으로 둠) — null로 둔다.
    public record ParsedSection(String title, int level, Integer pageFrom, int orderNo) {
    }
}
