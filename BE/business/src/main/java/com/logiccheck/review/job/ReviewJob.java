package com.logiccheck.review.job;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 문서 한 건에 대한 분석 작업.
 *
 * <p>진행률은 저장하지 않는다. 400ms마다 폴링이 들어오는데 그때마다 UPDATE를 하면
 * 쓰기가 과해져서, 시작 시각과 구간별 소요시간만 남기고 조회 시점에 계산한다.
 * 단계 전이(steps)와 부분 실패만 실제로 커밋한다.
 */
@Entity
@Table(name = "review_jobs")
public class ReviewJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다른 도메인의 엔티티를 직접 참조하지 않고 식별자만 들고 있는다 (Document.ownerId와 같은 방식).
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JobPhase phase;

    @Column(name = "parse_duration_ms", nullable = false)
    private int parseDurationMs;

    @Column(name = "analyze_duration_ms", nullable = false)
    private int analyzeDurationMs;

    @Column(name = "analyze_started_at")
    private Instant analyzeStartedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps")
    private List<JobStep> steps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "partial_failures")
    private List<PartialFailure> partialFailures;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    protected ReviewJob() {
    }

    private ReviewJob(Long documentId, int parseDurationMs, int analyzeDurationMs, List<JobStep> steps) {
        this.documentId = documentId;
        this.parseDurationMs = parseDurationMs;
        this.analyzeDurationMs = analyzeDurationMs;
        this.steps = steps;
        this.partialFailures = List.of();
        this.status = JobStatus.PENDING;
        this.reviewStatus = ReviewStatus.IN_REVIEW;
        this.phase = JobPhase.PARSE;
    }

    public static ReviewJob start(Long documentId, int parseDurationMs, int analyzeDurationMs, List<JobStep> steps) {
        return new ReviewJob(documentId, parseDurationMs, analyzeDurationMs, steps);
    }

    @PrePersist
    void onCreate() {
        startedAt = Instant.now();
    }

    public void beginParsing(List<JobStep> steps) {
        this.status = JobStatus.RUNNING;
        this.phase = JobPhase.PARSE;
        this.steps = steps;
    }

    public void advance(List<JobStep> steps) {
        this.steps = steps;
    }

    public void beginAnalyzing(List<JobStep> steps, List<PartialFailure> partialFailures) {
        this.phase = JobPhase.ANALYZE;
        this.steps = steps;
        this.partialFailures = partialFailures;
        this.analyzeStartedAt = Instant.now();
    }

    public void succeed() {
        this.status = JobStatus.DONE;
        this.finishedAt = Instant.now();
    }

    public void fail(String errorCode) {
        this.status = JobStatus.FAILED;
        this.errorCode = errorCode;
        this.finishedAt = Instant.now();
    }

    public void completeReview() {
        this.reviewStatus = ReviewStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() {
        return reviewStatus == ReviewStatus.COMPLETED;
    }

    /**
     * 파싱 구간 진행률(0~100). 경과 시간 기준이라 폴링할 때마다 자연스럽게 올라간다.
     * 분석 구간에 들어섰거나 이미 끝난 작업은 100으로 고정된다.
     */
    public int parseProgress() {
        if (phase == JobPhase.ANALYZE || status.isTerminal()) {
            return 100;
        }
        return percent(elapsedMillis(startedAt), parseDurationMs);
    }

    public int analyzeProgress() {
        if (status.isTerminal()) {
            return 100;
        }
        if (analyzeStartedAt == null) {
            return 0;
        }
        return percent(elapsedMillis(analyzeStartedAt), analyzeDurationMs);
    }

    private long elapsedMillis(Instant from) {
        return from == null ? 0 : Instant.now().toEpochMilli() - from.toEpochMilli();
    }

    private int percent(long elapsedMs, int durationMs) {
        if (durationMs <= 0) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, Math.round(elapsedMs * 100.0 / durationMs)));
    }

    public Long getId() {
        return id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public JobPhase getPhase() {
        return phase;
    }

    public List<JobStep> getSteps() {
        return steps == null ? List.of() : steps;
    }

    public List<PartialFailure> getPartialFailures() {
        return partialFailures == null ? List.of() : partialFailures;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
