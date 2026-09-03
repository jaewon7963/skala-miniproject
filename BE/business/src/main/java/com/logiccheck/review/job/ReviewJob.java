package com.logiccheck.review.job;

import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "review_jobs")
public class ReviewJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    /** 분석 시점의 ruleset 스냅샷. 파이프라인 착수 시 채운다 (DEV3 D-9). */
    @Column(name = "ruleset_version", length = 50)
    private String rulesetVersion;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ReviewJob() {
    }

    private ReviewJob(Long documentId) {
        this.documentId = documentId;
        this.status = JobStatus.PENDING;
        this.reviewStatus = ReviewStatus.IN_REVIEW;
    }

    /**
     * 명세 16 은 202 를 즉시 반환한다. 이 시점의 startedAt · finishedAt · rulesetVersion 은
     * 전부 null 이다 (DEV3 D-2).
     */
    public static ReviewJob pending(Long documentId) {
        return new ReviewJob(documentId);
    }

    /**
     * 명세 19. review_status 와 completed_at 만 바꾼다.
     * status 와 parse_status 는 건드리지 않는다 — 두 상태는 서로 독립이다 (DEV3 D-1 · D-8).
     */
    public void completeReview() {
        this.reviewStatus = ReviewStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isReviewCompleted() {
        return reviewStatus == ReviewStatus.COMPLETED;
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

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
