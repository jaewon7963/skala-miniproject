package com.logiccheck.document.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.logiccheck.document.entity.Document;

/**
 * 라이브러리 카드와 업로드 화면이 그대로 렌더링하는 문서 표현.
 *
 * <p>{@code tags}는 태그 식별자가 아니라 사람이 읽는 이름이다. 화면이 태그 사전을
 * 따로 받아오지 않아서 여기 내려준 문자열이 그대로 칩에 찍힌다.
 */
public record DocumentResponse(
        String id,
        String name,
        int version,
        Integer pageCount,
        long sizeBytes,
        String status,
        List<String> tags,
        String latestJobId,
        OffsetDateTime updatedAt,
        String summary) {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static DocumentResponse of(Document document, String status, List<String> tags, Long latestJobId) {
        return new DocumentResponse(
                String.valueOf(document.getId()),
                document.getTitle(),
                document.getVersion(),
                document.getPageCount(),
                document.getSizeBytes(),
                status,
                tags,
                latestJobId == null ? null : String.valueOf(latestJobId),
                document.getUpdatedAt().atOffset(KST),
                document.getSummary());
    }
}
