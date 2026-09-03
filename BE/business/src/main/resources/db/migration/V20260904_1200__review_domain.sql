-- DEV3 Analysis/Review 도메인 (담당 테이블 7종).
-- 개발자1의 baseline(users·documents)이 아직 없어 타 도메인 FK 는 걸지 않는다.
-- baseline 머지 후 별도 마이그레이션으로 다음 FK 를 추가한다:
--   review_jobs.document_id      -> documents(id)
--   finding_elements.element_id  -> extracted_elements(id)
--   findings.section_id          -> sections(id)
--   finding_decisions.actor_id   -> users(id)
--   annotations.author_id        -> users(id)

CREATE TABLE review_jobs (
    id              BIGSERIAL    PRIMARY KEY,
    document_id     BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    review_status   VARCHAR(20)  NOT NULL DEFAULT 'IN_REVIEW',
    ruleset_version VARCHAR(50),
    error_code      VARCHAR(50),
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_review_jobs_status        CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    CONSTRAINT ck_review_jobs_review_status CHECK (review_status IN ('IN_REVIEW', 'COMPLETED'))
);

-- 동시 실행 차단의 최종 방어선 (DEV3 D-2).
-- ERD 원문은 WHERE status = 'RUNNING' 이지만 신규 Job 이 PENDING 으로 생성되므로
-- PENDING 을 포함해야 "동시 요청 2건에 Job 1개" 를 보장할 수 있다.
CREATE UNIQUE INDEX ux_review_jobs_active
    ON review_jobs (document_id)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE INDEX ix_review_jobs_document_created ON review_jobs (document_id, created_at DESC);

-- 재현 가능성: 분석 시점의 ruleset 버전을 review_jobs.ruleset_version 에 스냅샷한다 (DEV3 D-9).
CREATE TABLE validation_rules (
    id              BIGSERIAL     PRIMARY KEY,
    code            VARCHAR(64)   NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    description     TEXT,
    expression      TEXT,
    tolerance       NUMERIC(12, 6),
    severity        VARCHAR(10)   NOT NULL,
    ruleset_version VARCHAR(50)   NOT NULL,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_validation_rules_severity CHECK (severity IN ('ERROR', 'WARNING', 'INFO')),
    CONSTRAINT uq_validation_rules_code_version UNIQUE (code, ruleset_version)
);

-- method 는 저장 컬럼이 아니라 rule_id 유무로 파생한다 (DEV3 D-4).
-- calculation 은 DETERMINISTIC 일 때만 채운다.
CREATE TABLE findings (
    id               BIGSERIAL     PRIMARY KEY,
    job_id           BIGINT        NOT NULL REFERENCES review_jobs (id) ON DELETE CASCADE,
    rule_id          BIGINT        REFERENCES validation_rules (id),
    severity         VARCHAR(10)   NOT NULL,
    status           VARCHAR(10)   NOT NULL DEFAULT 'OPEN',
    title            VARCHAR(300)  NOT NULL,
    description      TEXT,
    confidence       NUMERIC(4, 3),
    page_no          INTEGER,
    section_id       BIGINT,
    calc_expression  TEXT,
    calc_expected    TEXT,
    calc_actual      TEXT,
    calc_diff        TEXT,
    embedding        TEXT,
    decided_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_findings_severity   CHECK (severity IN ('ERROR', 'WARNING', 'INFO')),
    CONSTRAINT ck_findings_status     CHECK (status IN ('OPEN', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT ck_findings_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE INDEX ix_findings_job_severity ON findings (job_id, severity, confidence DESC);

-- 모든 Finding 은 최소 1건의 근거를 가진다. bbox 는 페이지 크기 대비 0~1 상대 좌표 (DEV3 D-5).
-- id 가 FE 하이라이트 앵커 키다.
CREATE TABLE finding_evidence (
    id         BIGSERIAL     PRIMARY KEY,
    finding_id BIGINT        NOT NULL REFERENCES findings (id) ON DELETE CASCADE,
    page_no    INTEGER       NOT NULL,
    quote      TEXT          NOT NULL,
    label      VARCHAR(300),
    bbox_x     NUMERIC(8, 6),
    bbox_y     NUMERIC(8, 6),
    bbox_w     NUMERIC(8, 6),
    bbox_h     NUMERIC(8, 6),
    char_start INTEGER,
    char_end   INTEGER,
    ordering   INTEGER       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_finding_evidence_bbox CHECK (
        (bbox_x IS NULL AND bbox_y IS NULL AND bbox_w IS NULL AND bbox_h IS NULL)
        OR (bbox_x BETWEEN 0 AND 1 AND bbox_y BETWEEN 0 AND 1
            AND bbox_w BETWEEN 0 AND 1 AND bbox_h BETWEEN 0 AND 1)
    )
);

CREATE INDEX ix_finding_evidence_finding ON finding_evidence (finding_id, ordering);

CREATE TABLE finding_elements (
    id         BIGSERIAL    PRIMARY KEY,
    finding_id BIGINT       NOT NULL REFERENCES findings (id) ON DELETE CASCADE,
    element_id BIGINT       NOT NULL,
    role       VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_finding_elements UNIQUE (finding_id, element_id)
);

-- 판정 이력은 누적한다. findings.status 만 갱신하고 이력을 남기지 않으면 안 된다 (DEV3 D-6).
CREATE TABLE finding_decisions (
    id            BIGSERIAL    PRIMARY KEY,
    finding_id    BIGINT       NOT NULL REFERENCES findings (id) ON DELETE CASCADE,
    actor_id      BIGINT       NOT NULL,
    action        VARCHAR(10)  NOT NULL,
    before_status VARCHAR(10)  NOT NULL,
    after_status  VARCHAR(10)  NOT NULL,
    note          TEXT,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_finding_decisions_action CHECK (action IN ('ACCEPT', 'REJECT'))
);

CREATE INDEX ix_finding_decisions_finding ON finding_decisions (finding_id, created_at DESC);

-- 단일 테이블. finding_id 유무로 PDF 자유 주석과 Finding 주석을 구분하므로
-- source/origin·color 컬럼을 두지 않는다 (DEV3 D-7).
CREATE TABLE annotations (
    id         BIGSERIAL     PRIMARY KEY,
    job_id     BIGINT        NOT NULL REFERENCES review_jobs (id) ON DELETE CASCADE,
    finding_id BIGINT        REFERENCES findings (id) ON DELETE CASCADE,
    author_id  BIGINT        NOT NULL,
    body       TEXT          NOT NULL,
    page_no    INTEGER,
    quote      TEXT,
    bbox_x     NUMERIC(8, 6),
    bbox_y     NUMERIC(8, 6),
    bbox_w     NUMERIC(8, 6),
    bbox_h     NUMERIC(8, 6),
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_annotations_bbox CHECK (
        (bbox_x IS NULL AND bbox_y IS NULL AND bbox_w IS NULL AND bbox_h IS NULL)
        OR (bbox_x BETWEEN 0 AND 1 AND bbox_y BETWEEN 0 AND 1
            AND bbox_w BETWEEN 0 AND 1 AND bbox_h BETWEEN 0 AND 1)
    )
);

CREATE INDEX ix_annotations_job ON annotations (job_id, created_at) WHERE deleted_at IS NULL;
CREATE INDEX ix_annotations_finding ON annotations (finding_id) WHERE deleted_at IS NULL;
