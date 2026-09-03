package com.logiccheck.ai;

import java.util.List;

/**
 * BE → AI 서버 (명세 8-2 기준, 이 프로젝트 ERD 에 맞춰 조정).
 *
 * 구 명세의 elements[].blockId 는 ERD 에 Block 테이블이 없어 제외했다.
 * 대신 elementId 로 근거를 되짚는다.
 */
public record ReviewAiRequest(
        String jobId,
        String documentTitle,
        String model,
        Double temperature,
        String promptVersion,
        List<String> criteria,
        List<Section> sections,
        List<Element> elements
) {

    public record Section(String id, String title, Integer page) {
    }

    /** type: NUMBER · CLAIM · TECH · KPI · TABLE (명세 8-2) */
    public record Element(String id, String type, String sectionId, Integer page, String text) {
    }
}
