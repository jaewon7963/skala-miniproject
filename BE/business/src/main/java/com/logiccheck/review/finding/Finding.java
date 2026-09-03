package com.logiccheck.review.finding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 분석이 찾아낸 검토 항목 한 건.
 *
 * <p>근거({@link FindingEvidence})가 하나도 없는 항목은 만들지 않는다.
 * 어디를 보라는 것인지 못 알려주는 지적은 화면에서 쓸모가 없다.
 *
 * <p>confidence는 DB가 NUMERIC이라 BigDecimal로 매핑한다. double로 두면
 * ddl-auto=validate가 DOUBLE PRECISION을 기대해 기동 시 실패한다.
 */
@Entity
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false, length = 20)
    private FindingType findingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Verdict verdict;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "page_no", nullable = false)
    private int pageNo;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation")
    private Calculation calculation;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "finding", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo asc")
    private List<FindingEvidence> evidence = new ArrayList<>();

    protected Finding() {
    }

    private Finding(Long jobId, FindingType findingType, FindingMethod method, Long sectionId, int pageNo,
                     String title, String description, BigDecimal confidence, Calculation calculation, int orderNo) {
        this.jobId = jobId;
        this.findingType = findingType;
        this.method = method;
        this.verdict = Verdict.PENDING;
        this.sectionId = sectionId;
        this.pageNo = pageNo;
        this.title = title;
        this.description = description;
        this.confidence = confidence;
        this.calculation = calculation;
        this.orderNo = orderNo;
    }

    public static Finding of(Long jobId, FindingType findingType, FindingMethod method, Long sectionId, int pageNo,
                              String title, String description, BigDecimal confidence, Calculation calculation,
                              int orderNo) {
        return new Finding(jobId, findingType, method, sectionId, pageNo, title, description, confidence, calculation,
                orderNo);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addEvidence(String anchorId, int pageNo, String label, String selectedText) {
        evidence.add(FindingEvidence.of(this, anchorId, pageNo, label, selectedText, evidence.size()));
    }

    public boolean hasEvidence() {
        return !evidence.isEmpty();
    }

    public void decide(Verdict verdict) {
        this.verdict = verdict;
        this.decidedAt = verdict == Verdict.PENDING ? null : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public FindingType getFindingType() {
        return findingType;
    }

    public FindingMethod getMethod() {
        return method;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public int getPageNo() {
        return pageNo;
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

    public Calculation getCalculation() {
        return calculation;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public List<FindingEvidence> getEvidence() {
        return evidence;
    }
}
