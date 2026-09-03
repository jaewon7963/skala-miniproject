package com.logiccheck.review.finding;

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

/**
 * 원문 근거. 이 행의 id 가 FE 하이라이트 앵커 키다 — 응답에서 빠뜨리면
 * 원문↔항목 점프가 동작하지 않는다 (DEV3 D-5).
 *
 * bbox 는 절대 픽셀이 아니라 페이지 크기 대비 0~1 상대 좌표다.
 * 렌더링 복원 순서는 bbox → 실패 시 quote 재탐색이므로 quote 는 반드시 채운다.
 */
@Entity
@Table(name = "finding_evidence")
public class FindingEvidence extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private Finding finding;

    @Column(name = "page_no", nullable = false)
    private Integer pageNo;

    @Column(name = "quote", nullable = false)
    private String quote;

    @Column(name = "label", length = 300)
    private String label;

    @Column(name = "bbox_x", precision = 8, scale = 6)
    private BigDecimal bboxX;

    @Column(name = "bbox_y", precision = 8, scale = 6)
    private BigDecimal bboxY;

    @Column(name = "bbox_w", precision = 8, scale = 6)
    private BigDecimal bboxW;

    @Column(name = "bbox_h", precision = 8, scale = 6)
    private BigDecimal bboxH;

    @Column(name = "char_start")
    private Integer charStart;

    @Column(name = "char_end")
    private Integer charEnd;

    @Column(name = "ordering", nullable = false)
    private Integer ordering;

    protected FindingEvidence() {
    }

    private FindingEvidence(Finding finding, Integer pageNo, String quote, String label,
                            BigDecimal bboxX, BigDecimal bboxY, BigDecimal bboxW, BigDecimal bboxH,
                            Integer charStart, Integer charEnd, Integer ordering) {
        this.finding = finding;
        this.pageNo = pageNo;
        this.quote = quote;
        this.label = label;
        this.bboxX = bboxX;
        this.bboxY = bboxY;
        this.bboxW = bboxW;
        this.bboxH = bboxH;
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.ordering = ordering;
    }

    /** bbox 는 네 값이 모두 있을 때만 저장한다 — DB CHECK 제약과 같은 규칙이다. */
    public static FindingEvidence of(Finding finding, Integer pageNo, String quote, String label,
                                     BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h,
                                     Integer charStart, Integer charEnd, int ordering) {
        boolean fullBbox = x != null && y != null && w != null && h != null;
        return new FindingEvidence(finding, pageNo, quote, label,
                fullBbox ? x : null, fullBbox ? y : null, fullBbox ? w : null, fullBbox ? h : null,
                charStart, charEnd, ordering);
    }

    /** bbox 네 값이 모두 있을 때만 좌표를 내려보낸다. */
    public boolean hasBbox() {
        return bboxX != null && bboxY != null && bboxW != null && bboxH != null;
    }

    public Long getId() {
        return id;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public String getQuote() {
        return quote;
    }

    public String getLabel() {
        return label;
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

    public Integer getCharStart() {
        return charStart;
    }

    public Integer getCharEnd() {
        return charEnd;
    }

    public Integer getOrdering() {
        return ordering;
    }
}
