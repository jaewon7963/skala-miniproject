package com.logiccheck.review.pipeline;

import com.logiccheck.ai.ReviewAiException;
import com.logiccheck.document.port.DocumentQueryPort;
import com.logiccheck.document.port.DocumentStructurePort;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.document.port.DocumentStructurePort.PageView;
import com.logiccheck.document.port.DocumentStructurePort.SectionView;
import com.logiccheck.review.rule.RulesetVersionResolver;
import com.logiccheck.review.rule.ValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 명세 16 의 백그라운드 파이프라인 (DEV3 D-10).
 *
 * 1. 문서 구조 조회       DocumentStructurePort
 * 2. 검증 규칙 조회       ValidationRule (ruleset_version 스냅샷)
 * 3. 결정적 검산          계산식·허용 오차 → Finding(rule_id 존재)
 * 6. 검토사항 저장        Finding
 * 7. 관련 요소 연결       FindingElement
 * 8. 원문 근거 저장       FindingEvidence
 *
 * 4. 관련 요소·문맥 검색   AiReviewStage 가 요소·섹션을 AI 서버에 넘긴다
 * 5. AI 관계 판단         → Finding(rule_id = null)
 *
 * 실패 시 status = FAILED 와 error_code 를 남긴다.
 */
@Component
public class ReviewPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipeline.class);

    private final ReviewPipelineStore store;
    private final DocumentStructurePort documentStructurePort;
    private final DocumentQueryPort documentQueryPort;
    private final RulesetVersionResolver rulesetVersionResolver;
    private final QuoteVerifier quoteVerifier;
    private final AiReviewStage aiReviewStage;
    private final Map<String, DeterministicChecker> checkersByRuleCode;

    public ReviewPipeline(ReviewPipelineStore store,
                          DocumentStructurePort documentStructurePort,
                          DocumentQueryPort documentQueryPort,
                          RulesetVersionResolver rulesetVersionResolver,
                          QuoteVerifier quoteVerifier,
                          AiReviewStage aiReviewStage,
                          List<DeterministicChecker> checkers) {
        this.store = store;
        this.documentStructurePort = documentStructurePort;
        this.documentQueryPort = documentQueryPort;
        this.rulesetVersionResolver = rulesetVersionResolver;
        this.quoteVerifier = quoteVerifier;
        this.aiReviewStage = aiReviewStage;
        this.checkersByRuleCode = checkers.stream()
                .collect(Collectors.toMap(DeterministicChecker::ruleCode, Function.identity()));
    }

    /**
     * 명세 16 은 202 를 즉시 반환한다. 이 메서드는 트랜잭션 커밋 후 별도 스레드에서 돈다.
     *
     * ownerId 는 Job 을 시작한 사용자다. DocumentQueryPort 는 소유자 기준으로만 조회를 허용하므로
     * (A-5 Port ①) 백그라운드에서도 문서 정보를 읽으려면 이 값이 필요하다.
     */
    @Async
    public void runAsync(Long jobId, Long ownerId) {
        run(jobId, ownerId);
    }

    void run(Long jobId, Long ownerId) {
        try {
            String rulesetVersion = rulesetVersionResolver.currentVersion();
            Optional<Long> documentId = store.startRunning(jobId, rulesetVersion);
            if (documentId.isEmpty()) {
                log.info("PENDING 상태가 아니어서 파이프라인을 건너뛴다. jobId={}", jobId);
                return;
            }

            List<PageView> pages = documentStructurePort.findPages(documentId.get());
            List<SectionView> sections = documentStructurePort.findSections(documentId.get());
            List<ElementView> elements = documentStructurePort.findElements(documentId.get());
            List<ValidationRule> rules = rulesetVersionResolver.rulesOf(rulesetVersion);

            QuoteVerifier.Verification verification = quoteVerifier.of(pages);

            // 결정적 검산 — 인용문이 추출 요소의 rawText 이므로 대조 불가 페이지도 신뢰한다
            List<FindingDraft> deterministic = runDeterministicChecks(elements, rules);
            List<FindingDraft> accepted = new ArrayList<>(deterministic.stream()
                    .filter(draft -> verification.accepts(draft, true))
                    .toList());

            // AI 관계 판단 — 대조 불가 페이지의 인용문은 폐기한다
            List<FindingDraft> fromAi = aiReviewStage.run(jobId,
                    titleOf(documentId.get(), ownerId), sections, elements);
            List<FindingDraft> acceptedFromAi = fromAi.stream()
                    .filter(draft -> verification.accepts(draft, false))
                    .toList();
            accepted.addAll(acceptedFromAi);

            int saved = store.saveFindings(jobId, accepted);
            store.markDone(jobId);
            log.info("파이프라인 완료. jobId={} ruleset={} 요소={} 결정적={}/{} AI={}/{} 저장={}",
                    jobId, rulesetVersion, elements.size(),
                    accepted.size() - acceptedFromAi.size(), deterministic.size(),
                    acceptedFromAi.size(), fromAi.size(), saved);
        } catch (ReviewAiException e) {
            log.error("AI 단계 실패. jobId={} errorCode={}", jobId, e.getErrorCode(), e);
            store.markFailed(jobId, e.getErrorCode());
        } catch (Exception e) {
            log.error("파이프라인 실패. jobId={}", jobId, e);
            store.markFailed(jobId, "PIPELINE_ERROR");
        }
    }

    /** AI 요청의 documentTitle. 문서 정보는 DocumentQueryPort 로만 가져온다 (DEV3 D-3). */
    private String titleOf(Long documentId, Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return documentQueryPort.findMetaForOwner(documentId, ownerId)
                .map(DocumentQueryPort.DocumentMetaView::title)
                .orElse(null);
    }

    private List<FindingDraft> runDeterministicChecks(List<ElementView> elements, List<ValidationRule> rules) {
        List<FindingDraft> drafts = new ArrayList<>();
        for (ValidationRule rule : rules) {
            DeterministicChecker checker = checkersByRuleCode.get(rule.getCode());
            if (checker == null) {
                log.debug("검산기가 없는 규칙은 건너뛴다. code={}", rule.getCode());
                continue;
            }
            drafts.addAll(checker.check(elements, rule));
        }
        return drafts;
    }
}
