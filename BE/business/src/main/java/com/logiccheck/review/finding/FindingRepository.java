package com.logiccheck.review.finding;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    /**
     * 명세 21. 한 Job 의 전체를 반환한다 — 필터·정렬 파라미터가 없다 (DEV3 D-4).
     * findings(job_id, severity, confidence DESC) 인덱스가 job_id 조회를 받는다.
     * severity 의 의미 순서(ERROR>WARNING>INFO)는 문자열 정렬과 다르므로
     * 최종 정렬은 FindingService 가 수행한다.
     */
    @EntityGraph(attributePaths = "evidence")
    List<Finding> findByJobId(Long jobId);

    @EntityGraph(attributePaths = "evidence")
    Optional<Finding> findWithEvidenceById(Long id);

    /** 명세 17 의 summary 집계. 스냅샷을 저장하지 않고 조회 시 계산한다 (DEV3 D-3). */
    @Query("""
            select new com.logiccheck.review.finding.SeverityStatusCount(f.severity, f.status, count(f))
            from Finding f
            where f.job.id = :jobId
            group by f.severity, f.status
            """)
    List<SeverityStatusCount> countBySeverityAndStatus(@Param("jobId") Long jobId);
}
