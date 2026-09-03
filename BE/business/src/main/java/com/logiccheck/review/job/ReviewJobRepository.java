package com.logiccheck.review.job;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewJobRepository extends JpaRepository<ReviewJob, Long> {

    boolean existsByDocumentIdAndStatusIn(Long documentId, Collection<JobStatus> statuses);

    Optional<ReviewJob> findFirstByDocumentIdOrderByIdDesc(Long documentId);

    /** 문서 목록의 상태 배지를 한 번에 계산하기 위한 문서별 최신 Job 1건씩. */
    @Query("""
            select j from ReviewJob j
            where j.documentId in :documentIds
              and j.id = (select max(o.id) from ReviewJob o where o.documentId = j.documentId)
            """)
    List<ReviewJob> findLatestByDocumentIds(@Param("documentIds") Collection<Long> documentIds);
}
