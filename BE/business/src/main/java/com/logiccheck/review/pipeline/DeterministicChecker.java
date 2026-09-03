package com.logiccheck.review.pipeline;

import com.logiccheck.document.port.DocumentStructurePort.ElementView;
import com.logiccheck.review.rule.ValidationRule;

import java.util.List;

/** 결정적 검산 (DEV3 D-10 3단계). 계산식·허용 오차로 Finding(rule_id 존재)을 만든다. */
public interface DeterministicChecker {

    /** 이 검산기가 담당하는 규칙 코드. */
    String ruleCode();

    List<FindingDraft> check(List<ElementView> elements, ValidationRule rule);
}
