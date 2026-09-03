package com.logiccheck.review.rule;

import com.logiccheck.review.finding.Severity;
import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 검증 규칙을 DB 에 버전으로 두고, 분석 시점 버전을 review_jobs.ruleset_version 에
 * 스냅샷으로 남겨 과거 판정을 그대로 재현한다 (DEV3 D-9).
 * 규칙이 나중에 바뀌어도 과거 Job 의 판정 근거가 유지된다.
 */
@Entity
@Table(name = "validation_rules")
public class ValidationRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    /** 계산식. 결정적 검산기가 해석한다 (DEV3 D-10 3단계). */
    @Column(name = "expression")
    private String expression;

    /** 허용 오차. 차이가 이 값 이하면 지적하지 않는다. */
    @Column(name = "tolerance", precision = 12, scale = 6)
    private BigDecimal tolerance;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    @Column(name = "ruleset_version", nullable = false, length = 50)
    private String rulesetVersion;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected ValidationRule() {
    }

    public BigDecimal toleranceOrZero() {
        return tolerance == null ? BigDecimal.ZERO : tolerance;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getExpression() {
        return expression;
    }

    public BigDecimal getTolerance() {
        return tolerance;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
