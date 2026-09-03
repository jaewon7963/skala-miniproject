package com.logiccheck.review.finding;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    /** 검토 화면이 한 번에 전부 받아 가므로 근거까지 같이 당겨온다 (N+1 방지). */
    @EntityGraph(attributePaths = "evidence")
    List<Finding> findByJobIdOrderByOrderNoAsc(Long jobId);

    @EntityGraph(attributePaths = "evidence")
    Optional<Finding> findByIdAndJobId(Long id, Long jobId);

    int countByJobId(Long jobId);
}
