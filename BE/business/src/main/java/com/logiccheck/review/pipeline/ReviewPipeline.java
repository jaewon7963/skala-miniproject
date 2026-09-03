package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort;
import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.document.port.DocumentStructurePort.PageView;
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
 * 4·5 (임베딩 검색 · AI 관계 판단)는 S5b 에서 붙인다.
 * 실패 시 status = FAILED 와 error_code 를 남긴다.
 */
@Component
public class ReviewPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipeline.class);

    private final ReviewPipelineStore store;
    private final DocumentStructurePort documentStructurePort;
    private final RulesetVersionResolver rulesetVersionResolver;
    private final QuoteVerifier quoteVerifier;
    private final Map<String, DeterministicChecker> checkersByRuleCode;

    public ReviewPipeline(ReviewPipelineStore store,
                          DocumentStructurePort documentStructurePort,
                          RulesetVersionResolver rulesetVersionResolver,
                          QuoteVerifier quoteVerifier,
                          List<DeterministicChecker> checkers) {
        this.store = store;
        this.documentStructurePort = documentStructurePort;
        this.rulesetVersionResolver = rulesetVersionResolver;
        this.quoteVerifier = quoteVerifier;
        this.checkersByRuleCode = checkers.stream()
                .collect(Collectors.toMap(DeterministicChecker::ruleCode, Function.identity()));
    }

    /** 명세 16 은 202 를 즉시 반환한다. 이 메서드는 트랜잭션 커밋 후 별도 스레드에서 돈다. */
    @Async
    public void runAsync(Long jobId) {
        run(jobId);
    }

    void run(Long jobId) {
        try {
            String rulesetVersion = rulesetVersionResolver.currentVersion();
            Optional<Long> documentId = store.startRunning(jobId, rulesetVersion);
            if (documentId.isEmpty()) {
                log.info("PENDING 상태가 아니어서 파이프라인을 건너뛴다. jobId={}", jobId);
                return;
            }

            List<PageView> pages = documentStructurePort.findPages(documentId.get());
            List<ElementView> elements = documentStructurePort.findElements(documentId.get());
            List<ValidationRule> rules = rulesetVersionResolver.rulesOf(rulesetVersion);

            List<FindingDraft> drafts = runDeterministicChecks(elements, rules);

            QuoteVerifier.Verification verification = quoteVerifier.of(pages);
            List<FindingDraft> accepted = drafts.stream()
                    .filter(draft -> verification.accepts(draft, true))
                    .toList();

            int saved = store.saveFindings(jobId, accepted);
            store.markDone(jobId);
            log.info("파이프라인 완료. jobId={} ruleset={} 요소={} 초안={} 저장={}",
                    jobId, rulesetVersion, elements.size(), drafts.size(), saved);
        } catch (Exception e) {
            log.error("파이프라인 실패. jobId={}", jobId, e);
            store.markFailed(jobId, "PIPELINE_ERROR");
        }
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
