package com.logiccheck.document.entity;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// width/height는 DB에 NUMERIC(정밀도 무제한)으로 정의돼 있어 BigDecimal로 매핑한다.
// double로 매핑하면 ddl-auto=validate가 DOUBLE PRECISION을 기대해 기동 시 실패한다.
@Entity
@Table(name = "pages")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "page_no", nullable = false)
    private int pageNo;

    private BigDecimal width;

    private BigDecimal height;

    @Column(name = "text_layer")
    private String textLayer;

    // 원문 뷰어가 쓰는 블록 배열. 텍스트 레이어를 문단/제목/표로 나눈 결과다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocks")
    private List<PageBlock> blocks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    protected Page() {
    }

    private Page(Document document, int pageNo, BigDecimal width, BigDecimal height, String textLayer) {
        this.document = document;
        this.pageNo = pageNo;
        this.width = width;
        this.height = height;
        this.textLayer = textLayer;
    }

    public static Page of(Document document, int pageNo, BigDecimal width, BigDecimal height, String textLayer) {
        return new Page(document, pageNo, width, height, textLayer);
    }

    public void attach(List<PageBlock> blocks, Section section) {
        this.blocks = blocks;
        this.section = section;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public int getPageNo() {
        return pageNo;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public String getTextLayer() {
        return textLayer;
    }

    public List<PageBlock> getBlocks() {
        return blocks;
    }

    public Section getSection() {
        return section;
    }
}
