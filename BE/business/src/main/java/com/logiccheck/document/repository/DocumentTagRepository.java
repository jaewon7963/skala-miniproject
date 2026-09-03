package com.logiccheck.document.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.logiccheck.document.entity.DocumentTag;
import com.logiccheck.document.entity.DocumentTagId;
import com.logiccheck.tag.entity.Tag;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTagId> {

    List<DocumentTag> findByDocument_Id(Long documentId);

    void deleteByDocument_Id(Long documentId);

    // dt.document.id를 스칼라로만 선택해 지연 프록시 초기화(N+1)를 피한다.
    @Query("select dt.document.id as documentId, dt.tag as tag from DocumentTag dt where dt.document.id in :documentIds")
    List<DocumentTagRow> findRowsByDocumentIdIn(@Param("documentIds") Collection<Long> documentIds);

    interface DocumentTagRow {
        Long getDocumentId();

        Tag getTag();
    }
}
