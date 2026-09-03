package com.logiccheck.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.logiccheck.document.entity.Page;

public interface PageRepository extends JpaRepository<Page, Long> {

    List<Page> findByDocument_IdOrderByPageNoAsc(Long documentId);

    /** 섹션 제목까지 같이 쓰는 곳을 위해 한 번에 당겨온다. 페이지마다 따로 조회하지 않기 위함. */
    @Query("""
            select p from Page p
            left join fetch p.section
            where p.document.id = :documentId
            order by p.pageNo asc
            """)
    List<Page> findWithSectionByDocumentId(@Param("documentId") Long documentId);
}
