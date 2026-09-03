package com.logiccheck.review.finding;

/**
 * 판단 방식. 저장 컬럼이 아니라 rule_id 유무로 파생한다 (DEV3 D-4).
 * rule_id != null → DETERMINISTIC · null → RAG
 */
public enum FindingMethod {

    DETERMINISTIC,
    RAG
}
