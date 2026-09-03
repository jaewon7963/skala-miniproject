package com.logiccheck.document.dto;

import com.logiccheck.document.entity.Page;

public record PageResponse(int pageNo, Double width, Double height, String textLayer) {

    public static PageResponse from(Page page) {
        Double width = page.getWidth() == null ? null : page.getWidth().doubleValue();
        Double height = page.getHeight() == null ? null : page.getHeight().doubleValue();
        return new PageResponse(page.getPageNo(), width, height, page.getTextLayer());
    }
}
