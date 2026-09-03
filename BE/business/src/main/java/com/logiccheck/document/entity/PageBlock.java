package com.logiccheck.document.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 원문 뷰어가 렌더링하는 블록 하나. pages.blocks(JSONB)에 배열로 저장되고
 * 그대로 API 응답에 실린다.
 *
 * <p>{@code id}는 {@code b-{페이지}-{순번}} 규칙이며 검토 항목 근거의 anchorId와
 * 정확히 같아야 화면의 양방향 하이라이트가 동작한다.
 *
 * <p>{@code kind}: {@code h2} · {@code p} · {@code figure}는 {@code text}를,
 * {@code table}은 {@code caption}/{@code head}/{@code rows}를 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageBlock(
        String id,
        String kind,
        String text,
        String caption,
        List<String> head,
        List<List<String>> rows) {

    public static PageBlock text(String id, String kind, String text) {
        return new PageBlock(id, kind, text, null, null, null);
    }

    public static PageBlock table(String id, String caption, List<String> head, List<List<String>> rows) {
        return new PageBlock(id, "table", null, caption, head, rows);
    }

    /**
     * JSONB 컬럼으로 그대로 직렬화되는 타입이라, 저장할 필드가 아닌 판별 메서드는
     * 반드시 제외해야 한다. is- 로 시작하면 Jackson이 속성으로 오해해 값을 써넣고,
     * 되읽을 때 생성자에 없는 필드라며 실패한다.
     */
    @JsonIgnore
    public boolean isTable() {
        return "table".equals(kind);
    }
}
