package com.logiccheck.review.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.logiccheck.ai.ReviewAiClient;
import com.logiccheck.ai.ReviewAiRequest;
import com.logiccheck.ai.ReviewAiResult;
import com.logiccheck.document.entity.PageBlock;
import com.logiccheck.document.port.DocumentQueryPort;
import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.document.port.DocumentStructurePort;
import com.logiccheck.document.port.DocumentStructurePort.PageContentView;

/**
 * 분석 작업을 백그라운드에서 실제로 돌린다.
 *
 * <p>업로드 직후 곧바로 분석을 누르면 파싱이 아직 끝나지 않았을 수 있다. 그래서 파싱을
 * 선행 조건으로 요구해 거절하는 대신, 이 안에서 끝날 때까지 기다렸다가 이어서 분석한다.
 * 사용자 입장에서는 "업로드 → 분석" 한 흐름이지 두 단계가 아니기 때문이다.
 *
 * <p>어떤 경로로 끝나든 작업은 DONE 또는 FAILED에 도달해야 한다. 진행 화면이 그때까지
 * 조회를 멈추지 않기 때문에, 중간에 예외로 빠져나가면 화면이 영원히 돈다.
 */
@Component
public class ReviewJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ReviewJobRunner.class);

    private static final long PARSE_POLL_INTERVAL_MS = 300;
    private static final long PARSE_WAIT_LIMIT_MS = 120_000;
    private static final int MAX_REPORTED_FAILURES = 5;

    private final DocumentQueryPort documentQueryPort;
    private final DocumentStructurePort documentStructurePort;
    private final ReviewAiClient reviewAiClient;
    private final ReviewJobStateWriter writer;

    public ReviewJobRunner(DocumentQueryPort documentQueryPort, DocumentStructurePort documentStructurePort,
                            ReviewAiClient reviewAiClient, ReviewJobStateWriter writer) {
        this.documentQueryPort = documentQueryPort;
        this.documentStructurePort = documentStructurePort;
        this.reviewAiClient = reviewAiClient;
        this.writer = writer;
    }

    /**
     * 작업 행이 커밋된 뒤에 시작한다. 커밋 전에 다른 스레드가 먼저 읽으면 방금 만든 작업을
     * 찾지 못하기 때문이다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStarted(ReviewJobStartedEvent event) {
        run(event.jobId(), event.documentId(), event.ownerId(), event.parseDurationMs(), event.analyzeDurationMs());
    }

    public void run(Long jobId, Long documentId, Long ownerId, int parseDurationMs, int analyzeDurationMs) {
        try {
            DocumentMetaView document = parse(jobId, documentId, ownerId, parseDurationMs);
            analyze(jobId, documentId, document, analyzeDurationMs);
            writer.succeed(jobId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writer.fail(jobId, "INTERRUPTED");
        } catch (ParseFailedException e) {
            log.warn("작업 {}: 문서 {} 파싱이 실패해 분석을 진행할 수 없습니다", jobId, documentId);
            writer.fail(jobId, "DOCUMENT_NOT_READY");
        } catch (Exception e) {
            log.error("작업 {} 실행 중 오류", jobId, e);
            writer.fail(jobId, "INTERNAL_SERVER_ERROR");
        }
    }

    private DocumentMetaView parse(Long jobId, Long documentId, Long ownerId, int parseDurationMs)
            throws InterruptedException {
        writer.beginParsing(jobId);

        long slice = Math.max(1, parseDurationMs / ReviewJobSteps.count());
        for (int index = 0; index < ReviewJobSteps.count(); index++) {
            writer.advance(jobId, ReviewJobSteps.running(index));
            Thread.sleep(slice);
            writer.advance(jobId, ReviewJobSteps.completedThrough(index));
        }
        return awaitParsed(documentId, ownerId);
    }

    /** 업로드 직후의 파싱이 끝날 때까지 기다린다. 이미 끝나 있으면 곧바로 돌아온다. */
    private DocumentMetaView awaitParsed(Long documentId, Long ownerId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PARSE_WAIT_LIMIT_MS;
        while (true) {
            DocumentMetaView document = documentQueryPort.findMetaForOwner(documentId, ownerId)
                    .orElseThrow(ParseFailedException::new);
            if ("DONE".equals(document.parseStatus())) {
                return document;
            }
            if ("FAILED".equals(document.parseStatus()) || System.currentTimeMillis() > deadline) {
                throw new ParseFailedException();
            }
            Thread.sleep(PARSE_POLL_INTERVAL_MS);
        }
    }

    private void analyze(Long jobId, Long documentId, DocumentMetaView document, int analyzeDurationMs)
            throws InterruptedException {
        List<PageContentView> pages = documentStructurePort.findPageContents(documentId);
        writer.beginAnalyzing(jobId, partialFailures(pages));

        ReviewAiResult result = reviewAiClient.analyze(toRequest(document, pages));
        List<ReviewAiResult.AiFinding> findings = result.findings();

        long slice = Math.max(1, analyzeDurationMs / Math.max(1, findings.size()));
        for (int index = 0; index < findings.size(); index++) {
            writer.saveFinding(jobId, findings.get(index), index);
            Thread.sleep(slice);
        }
        if (findings.isEmpty()) {
            Thread.sleep(Math.min(analyzeDurationMs, 1000));
        }
    }

    /**
     * 본문을 못 읽은 페이지를 사용자에게 알린다. 스캔 이미지로만 된 장표가 여기 걸린다.
     * 전부 나열하면 화면이 경고로 뒤덮이므로 앞쪽 몇 건만 남긴다.
     */
    private List<PartialFailure> partialFailures(List<PageContentView> pages) {
        List<PartialFailure> failures = new ArrayList<>();
        for (PageContentView page : pages) {
            if (failures.size() >= MAX_REPORTED_FAILURES) {
                break;
            }
            if (hasNoTextLayer(page)) {
                failures.add(new PartialFailure(page.pageNo(),
                        "텍스트 레이어가 없어 이 페이지는 분석에서 제외하고 진행했습니다"));
            }
        }
        return failures;
    }

    private boolean hasNoTextLayer(PageContentView page) {
        List<PageBlock> blocks = page.blocks();
        return blocks.size() == 1 && "figure".equals(blocks.get(0).kind());
    }

    private ReviewAiRequest toRequest(DocumentMetaView document, List<PageContentView> pages) {
        List<ReviewAiRequest.AnalyzedPage> analyzed = pages.stream()
                .map(page -> new ReviewAiRequest.AnalyzedPage(page.pageNo(), page.sectionId(), page.sectionTitle(),
                        page.blocks().stream().map(this::toBlock).toList()))
                .toList();
        return new ReviewAiRequest(document.title(), analyzed);
    }

    private ReviewAiRequest.AnalyzedBlock toBlock(PageBlock block) {
        return new ReviewAiRequest.AnalyzedBlock(block.id(), block.kind(), block.text(), block.caption(),
                Optional.ofNullable(block.head()).orElse(List.of()),
                Optional.ofNullable(block.rows()).orElse(List.of()));
    }

    /** 파싱이 끝나지 않아 분석을 이어갈 수 없는 상태. */
    private static class ParseFailedException extends RuntimeException {
    }
}
