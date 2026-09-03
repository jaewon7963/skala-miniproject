package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort.PageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 인용문 검증 (DEV3 D-5).
 *
 * "AI 가 생성한 인용문을 원문(pages.text_layer)과 대조해 불일치하면 Finding 을 저장하지 않는다.
 *  근거 없는 지적을 반환하지 않는 것이 이 서비스의 전제다."
 *
 * 원문 텍스트가 없는 페이지(이미지형 표 등)는 대조할 수 없다. 이때는
 * - 결정적 검산 결과는 남긴다. 인용문이 추출 요소의 rawText 이므로 원문에서 온 값이다.
 * - AI 결과는 폐기한다. 검증할 수 없는 AI 응답을 신뢰 데이터처럼 저장하지 않는다 (DEV3 D-10 §8.10).
 */
@Component
public class QuoteVerifier {

    private static final Logger log = LoggerFactory.getLogger(QuoteVerifier.class);

    public Verification of(List<PageView> pages) {
        Map<Integer, String> textByPage = new HashMap<>();
        for (PageView page : pages) {
            if (page.textLayer() != null) {
                textByPage.put(page.pageNo(), normalize(page.textLayer()));
            }
        }
        return new Verification(textByPage);
    }

    /** 공백 차이는 무시한다. PDF 텍스트 레이어는 줄바꿈·공백이 원문과 어긋나는 일이 흔하다. */
    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    public class Verification {

        private final Map<Integer, String> textByPage;

        private Verification(Map<Integer, String> textByPage) {
            this.textByPage = textByPage;
        }

        /**
         * 인용문이 전부 원문과 일치하면 true.
         * trustWhenUnverifiable 는 원문 텍스트가 없는 페이지를 통과시킬지 여부다.
         */
        public boolean accepts(FindingDraft draft, boolean trustWhenUnverifiable) {
            if (!draft.hasEvidence()) {
                return false;
            }
            for (FindingDraft.EvidenceDraft evidence : draft.evidence()) {
                if (evidence.quote() == null || evidence.quote().isBlank()) {
                    return false;
                }
                String source = textByPage.get(evidence.pageNo());
                if (source == null) {
                    if (!trustWhenUnverifiable) {
                        log.info("원문 텍스트가 없어 검토사항을 폐기한다. page={} title={}",
                                evidence.pageNo(), draft.title());
                        return false;
                    }
                    continue;
                }
                if (!source.contains(normalize(evidence.quote()))) {
                    log.info("인용문이 원문과 불일치해 검토사항을 폐기한다. page={} title={}",
                            evidence.pageNo(), draft.title());
                    return false;
                }
            }
            return true;
        }
    }
}
