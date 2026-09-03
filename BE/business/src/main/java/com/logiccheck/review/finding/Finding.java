package com.logiccheck.review.finding;

import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * findings 테이블.
 *
 * embedding 컬럼은 일부러 매핑하지 않는다. 매핑하지 않으면 응답에 새어 나갈 수 없다 (DEV3 D-4).
 * calculation 은 JSONB 대신 calc_* 네 컬럼으로 나뉘어 있다 — 사유는 work_log.md S0 참고.
 */
@Entity
@Table(name = "findings")
public class Finding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private ReviewJob job;

    /** null 이면 RAG 판단, 값이 있으면 결정적 검산이다. */
    @Column(name = "rule_id")
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FindingStatus status;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "calc_expression")
    private String calcExpression;

    @Column(name = "calc_expected")
    private String calcExpected;

    @Column(name = "calc_actual")
    private String calcActual;

    @Column(name = "calc_diff")
    private String calcDiff;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    /** 모든 Finding 은 최소 1건의 근거를 가진다 (DEV3 D-5). */
    @OneToMany(mappedBy = "finding", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering asc, id asc")
    private List<FindingEvidence> evidence = new ArrayList<>();

    protected Finding() {
    }

    public FindingMethod method() {
        return ruleId != null ? FindingMethod.DETERMINISTIC : FindingMethod.RAG;
    }

    public Long getId() {
        return id;
    }

    public ReviewJob getJob() {
        return job;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getCalcExpression() {
        return calcExpression;
    }

    public String getCalcExpected() {
        return calcExpected;
    }

    public String getCalcActual() {
        return calcActual;
    }

    public String getCalcDiff() {
        return calcDiff;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public List<FindingEvidence> getEvidence() {
        return evidence;
    }
}
