package com.logiccheck.document.entity;

import java.io.Serializable;
import java.util.Objects;

// document_tags 복합 PK(document_id, tag_id)용 IdClass.
// 필드명이 DocumentTag의 @Id 필드명(document/tag)과 정확히 일치해야 한다.
public class DocumentTagId implements Serializable {
    private Long document;
    private Long tag;

    public DocumentTagId() {
    }

    public DocumentTagId(Long document, Long tag) {
        this.document = document;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentTagId that)) {
            return false;
        }
        return Objects.equals(document, that.document) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(document, tag);
    }
}
