package com.logiccheck.review.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

    List<ValidationRule> findByRulesetVersionAndEnabledIsTrue(String rulesetVersion);

    /**
     * 현재 ruleset 버전. 활성 규칙이 가진 버전 중 가장 큰 값을 쓴다.
     * 버전 문자열은 날짜 기반(ruleset-2026.09.01)이라 문자열 순서가 시간 순서와 일치한다.
     */
    @Query("select max(r.rulesetVersion) from ValidationRule r where r.enabled = true")
    Optional<String> findCurrentRulesetVersion();
}
