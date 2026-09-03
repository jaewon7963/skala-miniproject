package com.logiccheck.review.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewJobRepository extends JpaRepository<ReviewJob, Long> {

    boolean existsByDocumentIdAndStatusIn(Long documentId, Collection<JobStatus> statuses);

    Optional<ReviewJob> findFirstByDocumentIdOrderByIdDesc(Long documentId);

    /**
     * 문서별 최신 Job 1건을 단일 쿼리로 가져온다. Port ③ 의 구현에 쓰인다.
     * 단건 조회를 반복하면 개발자2 쪽 문서 목록에서 N+1 이 발생한다 (DEV3 E-1).
     */
    @Query("""
            select j
            from ReviewJob j
            where j.documentId in :documentIds
              and j.id = (select max(latest.id)
                          from ReviewJob latest
                          where latest.documentId = j.documentId)
            """)
    List<ReviewJob> findLatestOfEachDocument(@Param("documentIds") Collection<Long> documentIds);
}
