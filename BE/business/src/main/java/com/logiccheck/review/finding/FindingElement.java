package com.logiccheck.review.finding;

import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Finding 과 개발자2의 extracted_elements 를 잇는다 (DEV3 D-10 7단계).
 * element_id 는 타 도메인 PK 라 baseline 머지 후 FK 를 추가한다.
 * MVP1 응답에는 포함하지 않는다 — 파이프라인(S5)에서 채운다.
 */
@Entity
@Table(name = "finding_elements")
public class FindingElement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private Finding finding;

    @Column(name = "element_id", nullable = false)
    private Long elementId;

    @Column(name = "role", length = 30)
    private String role;

    protected FindingElement() {
    }

    public Long getId() {
        return id;
    }

    public Long getElementId() {
        return elementId;
    }

    public String getRole() {
        return role;
    }
}
