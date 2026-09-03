package com.logiccheck.document.dto;

import java.util.List;

public record DocumentListResponse(
        List<DocumentResponse> items, long total, int page, int size, CountsResponse counts) {
}
