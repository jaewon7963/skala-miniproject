package com.logiccheck.review.rule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 파이프라인 착수 시 현재 ruleset 버전을 결정한다 (DEV3 D-9). */
@Component
public class RulesetVersionResolver {

    private final ValidationRuleRepository validationRuleRepository;
    private final String fallbackVersion;

    public RulesetVersionResolver(ValidationRuleRepository validationRuleRepository,
                                  @Value("${review.ruleset.fallback-version:ruleset-empty}")
                                  String fallbackVersion) {
        this.validationRuleRepository = validationRuleRepository;
        this.fallbackVersion = fallbackVersion;
    }

    /** 활성 규칙이 하나도 없으면 fallback 을 쓴다 — Job 의 ruleset_version 은 NULL 로 남기지 않는다. */
    @Transactional(readOnly = true)
    public String currentVersion() {
        return validationRuleRepository.findCurrentRulesetVersion().orElse(fallbackVersion);
    }

    @Transactional(readOnly = true)
    public List<ValidationRule> rulesOf(String rulesetVersion) {
        return validationRuleRepository.findByRulesetVersionAndEnabledIsTrue(rulesetVersion);
    }
}
