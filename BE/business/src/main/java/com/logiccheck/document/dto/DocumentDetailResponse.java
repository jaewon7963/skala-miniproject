package com.logiccheck.document.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.logiccheck.document.entity.Document;
import com.logiccheck.tag.dto.TagResponse;

public record DocumentDetailResponse(
        String id, String title, Integer pageCount, long sizeBytes, String mimeType,
        String displayStatus, List<TagResponse> tags,
        OffsetDateTime uploadedAt, OffsetDateTime updatedAt) {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static DocumentDetailResponse of(Document d, String displayStatus, List<TagResponse> tags) {
        return new DocumentDetailResponse(String.valueOf(d.getId()), d.getTitle(), d.getPageCount(),
                d.getSizeBytes(), d.getMimeType(), displayStatus, tags,
                d.getUploadedAt().atOffset(KST), d.getUpdatedAt().atOffset(KST));
    }
}
