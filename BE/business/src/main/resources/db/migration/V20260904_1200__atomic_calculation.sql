-- 검산 근거를 원자 컬럼으로 되돌리고, 진행 단계에서 불변 리터럴을 뺀다.
--
-- 1. findings.calculation 은 JSONB 하나에 식·기댓값·실제값·차이를 담고 있었는데,
--    값이 "36.8억" 같은 표시용 문자열이라 숫자로 비교하거나 집계할 수 없었다.
--    baseline 이 갖고 있던 calc_expr / expected_value / actual_value 구조로 되돌린다.
--    차이값은 두 값의 뺄셈이라 저장하지 않고 조회 시 계산한다.
--
-- 2. review_jobs.steps 는 단계마다 key/label/detail 을 함께 저장했는데 셋 다 코드에
--    고정된 문자열이다. 작업 행마다 같은 값을 다섯 벌씩 복사하고 있었다.
--    실제로 변하는 state 만 남기고 나머지는 조회할 때 붙인다.

ALTER TABLE findings ADD COLUMN calc_expression TEXT;
ALTER TABLE findings ADD COLUMN calc_expected NUMERIC;
ALTER TABLE findings ADD COLUMN calc_actual NUMERIC;
ALTER TABLE findings DROP COLUMN calculation;

UPDATE review_jobs
SET steps = (
        SELECT jsonb_agg(elem ->> 'state' ORDER BY ord)
        FROM jsonb_array_elements(steps) WITH ORDINALITY AS t(elem, ord)
    )
WHERE steps IS NOT NULL
  AND jsonb_typeof(steps) = 'array'
  AND jsonb_array_length(steps) > 0
  AND jsonb_typeof(steps -> 0) = 'object';
