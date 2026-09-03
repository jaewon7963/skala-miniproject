package com.logiccheck.review.annotation;

import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * ERD 기준 annotations 단일 테이블 (DEV3 D-7).
 *
 * - source/origin 필드를 두지 않는다 — finding_id 유무로 완전히 구분된다.
 *   finding_id = null → PDF 자유 주석 · 값 존재 → Finding 주석
 * - color 필드를 두지 않는다 — MVP1 미지원이며 ERD 에 컬럼이 없다.
 * - job_id 가 NOT NULL 이므로 ReviewJob 생성 이후에만 만들 수 있다.
 * - author_id 는 JWT 사용자로 고정한다. 요청에서 받지 않는다.
 */
@Entity
@Table(name = "annotations")
public class Annotation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private ReviewJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private Finding finding;

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "quote")
    private String quote;

    @Column(name = "bbox_x", precision = 8, scale = 6)
    private BigDecimal bboxX;

    @Column(name = "bbox_y", precision = 8, scale = 6)
    private BigDecimal bboxY;

    @Column(name = "bbox_w", precision = 8, scale = 6)
    private BigDecimal bboxW;

    @Column(name = "bbox_h", precision = 8, scale = 6)
    private BigDecimal bboxH;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Annotation() {
    }

    private Annotation(ReviewJob job, Finding finding, Long authorId, String body, Anchor anchor) {
        this.job = job;
        this.finding = finding;
        this.authorId = authorId;
        this.body = body;
        if (anchor != null) {
            this.pageNo = anchor.pageNo();
            this.quote = anchor.quote();
            this.bboxX = anchor.x();
            this.bboxY = anchor.y();
            this.bboxW = anchor.w();
            this.bboxH = anchor.h();
        }
    }

    public static Annotation create(ReviewJob job, Finding finding, Long authorId, String body, Anchor anchor) {
        return new Annotation(job, finding, authorId, body, anchor);
    }

    public void changeBody(String body) {
        this.body = body;
    }

    /** soft delete. 조회는 deleted_at IS NULL 만 본다 (DEV3 D-7). */
    public void delete() {
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean hasBbox() {
        return bboxX != null && bboxY != null && bboxW != null && bboxH != null;
    }

    public Long getId() {
        return id;
    }

    public ReviewJob getJob() {
        return job;
    }

    public Finding getFinding() {
        return finding;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public String getQuote() {
        return quote;
    }

    public BigDecimal getBboxX() {
        return bboxX;
    }

    public BigDecimal getBboxY() {
        return bboxY;
    }

    public BigDecimal getBboxW() {
        return bboxW;
    }

    public BigDecimal getBboxH() {
        return bboxH;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    /** 좌표와 인용문. Finding 주석이면 해당 Finding 의 첫 evidence 를 복사한다. */
    public record Anchor(Integer pageNo, String quote,
                         BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h) {
    }
}
