package com.logiccheck.review.finding;

import com.logiccheck.review.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 판정 이력. findings.status 만 갱신하고 이력을 남기지 않으면 안 된다 (DEV3 D-6).
 * 판정 이력 조회 API 는 보류이므로 읽기 경로는 아직 없다.
 */
@Entity
@Table(name = "finding_decisions")
public class FindingDecision extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false, updatable = false)
    private Finding finding;

    /** JWT 사용자. 요청에서 받지 않는다. */
    @Column(name = "actor_id", nullable = false, updatable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10)
    private DecisionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", nullable = false, length = 10)
    private FindingStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status", nullable = false, length = 10)
    private FindingStatus afterStatus;

    @Column(name = "note")
    private String note;

    protected FindingDecision() {
    }

    private FindingDecision(Finding finding, Long actorId, DecisionAction action,
                            FindingStatus beforeStatus, FindingStatus afterStatus, String note) {
        this.finding = finding;
        this.actorId = actorId;
        this.action = action;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.note = note;
    }

    public static FindingDecision of(Finding finding, Long actorId, DecisionAction action,
                                     FindingStatus beforeStatus, String note) {
        return new FindingDecision(finding, actorId, action, beforeStatus, action.resultStatus(), note);
    }

    public Long getId() {
        return id;
    }

    public Finding getFinding() {
        return finding;
    }

    public Long getActorId() {
        return actorId;
    }

    public DecisionAction getAction() {
        return action;
    }

    public FindingStatus getBeforeStatus() {
        return beforeStatus;
    }

    public FindingStatus getAfterStatus() {
        return afterStatus;
    }

    public String getNote() {
        return note;
    }
}
