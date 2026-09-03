package com.logiccheck.ai;

import java.util.List;

/**
 * 모델에 넘기는 분석 입력.
 *
 * <p>{@code pages}는 원문을 제목·문단·표로 나눈 결과이며, 각 블록의 id가 곧 근거의 앵커가 된다.
 */
public record ReviewAiRequest(String documentTitle, List<AnalyzedPage> pages) {

    public record AnalyzedPage(int pageNo, Long sectionId, String sectionTitle, List<AnalyzedBlock> blocks) {
    }

    public record AnalyzedBlock(
            String id,
            String kind,
            String text,
            String caption,
            List<String> head,
            List<List<String>> rows) {

        public boolean isTable() {
            return "table".equals(kind);
        }
    }
}
