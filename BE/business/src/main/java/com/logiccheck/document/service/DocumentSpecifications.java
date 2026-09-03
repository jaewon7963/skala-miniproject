package com.logiccheck.document.service;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.DocumentTag;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    public static Specification<Document> ownerIs(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Document> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Document> titleContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Document> updatedAfter(Instant cutoff) {
        if (cutoff == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("updatedAt"), cutoff);
    }

    // document_tags를 엔티티 연관관계 대신 별도 조인 테이블 엔티티로 관리하므로
    // (created_at NOT NULL 제약 때문 — DocumentTag 참고) 서브쿼리로 필터링한다.
    public static Specification<Document> hasTag(Long tagId) {
        if (tagId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<DocumentTag> dt = sub.from(DocumentTag.class);
            sub.select(dt.get("document").get("id"))
                    .where(cb.equal(dt.get("tag").get("id"), tagId));
            return root.get("id").in(sub);
        };
    }
}
