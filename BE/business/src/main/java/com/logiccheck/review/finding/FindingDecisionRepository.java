package com.logiccheck.review.finding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingDecisionRepository extends JpaRepository<FindingDecision, Long> {

    List<FindingDecision> findByFindingIdOrderByIdAsc(Long findingId);
}
