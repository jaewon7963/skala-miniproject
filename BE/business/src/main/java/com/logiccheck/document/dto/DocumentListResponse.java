package com.logiccheck.document.dto;

import java.util.List;

public record DocumentListResponse(
        List<DocumentSummaryResponse> items, long total, int page, int size, CountsResponse counts) {
}
