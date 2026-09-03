package com.logiccheck.review.finding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 검토 항목이 가리키는 원문 위치.
 *
 * <p>{@code anchorId}는 원문 블록 id({@code b-{페이지}-{순번}})와 정확히 같아야 한다.
 * 원문 뷰어가 이 값으로 DOM을 찾아 하이라이트하므로, 어긋나면 항목↔원문 점프가 조용히 죽는다.
 */
@Entity
@Table(name = "finding_evidence")
public class FindingEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(name = "anchor_id", nullable = false, length = 100)
    private String anchorId;

    @Column(name = "page_no", nullable = false)
    private int pageNo;

    @Column(name = "label")
    private String label;

    @Column(name = "selected_text")
    private String selectedText;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    protected FindingEvidence() {
    }

    private FindingEvidence(Finding finding, String anchorId, int pageNo, String label, String selectedText,
                             int orderNo) {
        this.finding = finding;
        this.anchorId = anchorId;
        this.pageNo = pageNo;
        this.label = label;
        this.selectedText = selectedText;
        this.orderNo = orderNo;
    }

    static FindingEvidence of(Finding finding, String anchorId, int pageNo, String label, String selectedText,
                               int orderNo) {
        return new FindingEvidence(finding, anchorId, pageNo, label, selectedText, orderNo);
    }

    public Long getId() {
        return id;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public int getPageNo() {
        return pageNo;
    }

    public String getLabel() {
        return label;
    }

    public String getSelectedText() {
        return selectedText;
    }

    public int getOrderNo() {
        return orderNo;
    }
}
