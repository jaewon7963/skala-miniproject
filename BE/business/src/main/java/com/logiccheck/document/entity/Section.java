package com.logiccheck.document.entity;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "sections")
public class Section {

    public enum Source {
        ORIGINAL, EXTRACTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Section parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int level;

    @Column(name = "page_from")
    private Integer pageFrom;

    @Column(name = "page_to")
    private Integer pageTo;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    protected Section() {
    }

    private Section(Document document, Section parent, Source source, String title, int level, Integer pageFrom,
                     Integer pageTo, int orderNo) {
        this.document = document;
        this.parent = parent;
        this.source = source;
        this.title = title;
        this.level = level;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.orderNo = orderNo;
    }

    public static Section of(Document document, Section parent, Source source, String title, int level,
                              Integer pageFrom, Integer pageTo, int orderNo) {
        return new Section(document, parent, source, title, level, pageFrom, pageTo, orderNo);
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public Section getParent() {
        return parent;
    }

    public Source getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public int getLevel() {
        return level;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public Integer getPageTo() {
        return pageTo;
    }

    public int getOrderNo() {
        return orderNo;
    }
}
