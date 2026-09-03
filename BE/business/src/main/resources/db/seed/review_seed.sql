-- AI 파이프라인 없이 명세 21 · 22 를 검증하기 위한 시드 (DEV3 C-3 7단계).
-- db/migration 이 아니라 db/seed 에 둔다 — Flyway 가 집어가지 않아 운영에 유입되지 않는다.
-- seed 프로파일에서만 실행된다: --spring.profiles.active=stub,seed
-- document_id = 900001 로 표시해 두고 실행마다 지우고 다시 만든다.

DELETE FROM review_jobs WHERE document_id = 900001;

INSERT INTO review_jobs (document_id, status, review_status, ruleset_version,
                         started_at, finished_at, created_at, updated_at)
VALUES (900001, 'DONE', 'IN_REVIEW', 'ruleset-2026.09.01',
        now() - interval '3 minutes', now() - interval '1 minute', now(), now());

-- 결정적 검산 (rule_id 존재 → method = DETERMINISTIC, calculation 채움)
INSERT INTO validation_rules (code, name, description, expression, tolerance, severity,
                              ruleset_version, enabled, created_at, updated_at)
VALUES ('REVENUE_SUM', '연도별 매출 합계 일치', '본문 합계와 표 소계를 대조한다',
        'sum(yearly_revenue) = total_revenue', 0.01, 'ERROR', 'ruleset-2026.09.01', TRUE, now(), now()),
       ('LABOR_COST', '인건비 산출식 일치', '인원 x 기간 x 월단가 재계산',
        'headcount * months * monthly_rate = labor_cost', 0.01, 'ERROR', 'ruleset-2026.09.01', TRUE, now(), now())
ON CONFLICT (code, ruleset_version) DO NOTHING;

WITH job AS (SELECT id FROM review_jobs WHERE document_id = 900001),
     rule_revenue AS (SELECT id FROM validation_rules WHERE code = 'REVENUE_SUM' AND ruleset_version = 'ruleset-2026.09.01'),
     rule_labor AS (SELECT id FROM validation_rules WHERE code = 'LABOR_COST' AND ruleset_version = 'ruleset-2026.09.01'),
     inserted AS (
         INSERT INTO findings (job_id, rule_id, severity, status, title, description, confidence,
                               page_no, section_id, calc_expression, calc_expected, calc_actual, calc_diff,
                               created_at, updated_at)
         VALUES
             ((SELECT id FROM job), (SELECT id FROM rule_revenue), 'ERROR', 'OPEN',
              '매출 합계와 표 5-1 소계가 불일치합니다',
              '본문 2027년 매출 24억 원과 표 5-1 합계 36.8억 원의 연도별 값이 맞지 않습니다',
              0.960, 11, NULL, '3.2 + 9.6 + 24', '36.8억', '24억', '3,200만 원', now(), now()),
             ((SELECT id FROM job), (SELECT id FROM rule_labor), 'ERROR', 'OPEN',
              '인건비 합계가 산출식과 맞지 않습니다',
              '인원 x 참여기간 x 월 단가로 재계산한 값이 기재 금액보다 2,400만 원 적습니다',
              0.910, 14, NULL, '5 * 8 * 450', '1억 8,000만 원', '2억 400만 원', '2,400만 원', now(), now()),
             -- RAG 판단 (rule_id = NULL → method = RAG, calculation 은 null 이어야 한다)
             ((SELECT id FROM job), NULL, 'WARNING', 'OPEN',
              '실시간 분석 목표와 기술 구성을 확인해야 합니다',
              '0.5초 응답 목표에 비해 대형 비전 모델 3종을 순차 실행하며 처리량 근거가 없습니다',
              0.780, 19, NULL, NULL, NULL, NULL, NULL, now(), now()),
             ((SELECT id FROM job), NULL, 'WARNING', 'OPEN',
              '목표 고객과 판매 채널이 일치하지 않습니다',
              '최종 사용자는 개인 고객이지만 판매 계획은 기업 직접 영업만 제시합니다',
              0.840, 5, NULL, NULL, NULL, NULL, NULL, now(), now()),
             ((SELECT id FROM job), NULL, 'INFO', 'OPEN',
              '정확도 KPI 의 평가 방법이 없습니다',
              '정확도 95% 의 시험 데이터 규모와 측정 지표가 정의되지 않았습니다',
              0.650, 19, NULL, NULL, NULL, NULL, NULL, now(), now())
         RETURNING id, page_no, title
     )
-- 모든 Finding 은 최소 1건의 근거를 가진다 (DEV3 D-5). bbox 는 0~1 상대 좌표.
INSERT INTO finding_evidence (finding_id, page_no, quote, label,
                              bbox_x, bbox_y, bbox_w, bbox_h, char_start, char_end,
                              ordering, created_at, updated_at)
SELECT id, page_no,
       '2027년 매출 24억 원',
       '본문 문단 · p.' || page_no,
       0.120000, 0.310000, 0.660000, 0.030000, 1240, 1256,
       0, now(), now()
FROM inserted;
