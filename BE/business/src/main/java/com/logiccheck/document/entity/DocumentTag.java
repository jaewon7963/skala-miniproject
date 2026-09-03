package com.logiccheck.document.entity;

import java.time.Instant;

import com.logiccheck.tag.entity.Tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

// document_tags.created_at이 NOT NULL(기본값 없음)이라 암시적 @ManyToMany 조인테이블로는
// INSERT가 실패한다(Hibernate가 document_id/tag_id만 채워 넣음) — 그래서 직접 매핑한다.
@Entity
@Table(name = "document_tags")
@IdClass(DocumentTagId.class)
public class DocumentTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentTag() {
    }

    private DocumentTag(Document document, Tag tag) {
        this.document = document;
        this.tag = tag;
    }

    public static DocumentTag of(Document document, Tag tag) {
        return new DocumentTag(document, tag);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Document getDocument() {
        return document;
    }

    public Tag getTag() {
        return tag;
    }
}
