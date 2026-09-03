package com.logiccheck.review.finding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingElementRepository extends JpaRepository<FindingElement, Long> {

    List<FindingElement> findByFindingId(Long findingId);
}
