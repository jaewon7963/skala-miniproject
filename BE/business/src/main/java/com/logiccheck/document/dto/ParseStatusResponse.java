package com.logiccheck.document.dto;

import com.logiccheck.document.entity.Document;

public record ParseStatusResponse(String parseStatus, Integer pageCount) {

    public static ParseStatusResponse from(Document d) {
        return new ParseStatusResponse(d.getParseStatus().name(), d.getPageCount());
    }
}
