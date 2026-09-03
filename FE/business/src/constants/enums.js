/**
 * 도메인 enum 과 화면 표기 라벨.
 * 서버 응답 값과 1:1 로 맞춥니다. 값이 바뀌면 이 파일만 수정하면 됩니다.
 *
 * NOTE(기획 확정 필요)
 *  - 검토 결과 유형: 와이어프레임 기준 3종으로 두었습니다.
 *    PRD 문구(오류/불일치/누락/확인 필요) 4종으로 확정되면 FINDING_TYPE 만 교체하세요.
 *  - 판정: 와이어프레임 기준 2종 + 되돌리기. 기획서의 '보류'는 HOLD 로 주석 처리해 두었습니다.
 */

/* ---------------- 문서 ---------------- */
export const DOC_STATUS = {
  IDLE: 'IDLE', // 업로드만 됨(미분석)
  PARSING: 'PARSING',
  ANALYZING: 'ANALYZING',
  REVIEWING: 'REVIEWING', // 검토 중
  DONE: 'DONE', // 검토 완료
  FAILED: 'FAILED', // 파싱 실패
}

export const DOC_STATUS_LABEL = {
  [DOC_STATUS.IDLE]: '미분석',
  [DOC_STATUS.PARSING]: '파싱 중',
  [DOC_STATUS.ANALYZING]: '분석 중',
  [DOC_STATUS.REVIEWING]: '검토 중',
  [DOC_STATUS.DONE]: '검토 완료',
  [DOC_STATUS.FAILED]: '파싱 실패',
}

/* ---------------- 분석 작업(ReviewJob) ---------------- */
export const JOB_STATUS = {
  PENDING: 'PENDING',
  RUNNING: 'RUNNING',
  PARTIAL: 'PARTIAL', // 부분 실패 후 계속 진행
  DONE: 'DONE',
  FAILED: 'FAILED',
}

export const JOB_STATUS_LABEL = {
  [JOB_STATUS.PENDING]: '대기',
  [JOB_STATUS.RUNNING]: '처리 중',
  [JOB_STATUS.PARTIAL]: '부분 실패',
  [JOB_STATUS.DONE]: '완료',
  [JOB_STATUS.FAILED]: '실패',
}

/* ---------------- 검토 결과(Finding) ---------------- */
export const FINDING_TYPE = {
  ERROR: 'ERROR',
  NEEDS_CHECK: 'NEEDS_CHECK',
  NO_EVIDENCE: 'NO_EVIDENCE',
}

export const FINDING_TYPE_LABEL = {
  [FINDING_TYPE.ERROR]: '오류',
  [FINDING_TYPE.NEEDS_CHECK]: '확인 필요',
  [FINDING_TYPE.NO_EVIDENCE]: '근거 부족',
}

export const FINDING_TYPE_ORDER = [
  FINDING_TYPE.ERROR,
  FINDING_TYPE.NEEDS_CHECK,
  FINDING_TYPE.NO_EVIDENCE,
]

/* 판단 방식 : 결정적 검산 / RAG 관계 판단 */
export const FINDING_METHOD = {
  DETERMINISTIC: 'DETERMINISTIC',
  RAG: 'RAG',
  MANUAL: 'MANUAL',
}

export const FINDING_METHOD_LABEL = {
  [FINDING_METHOD.DETERMINISTIC]: '결정적 검산',
  [FINDING_METHOD.RAG]: 'RAG 관계 판단',
  [FINDING_METHOD.MANUAL]: '사용자 등록',
}

/* ---------------- 사용자 판정(Verdict) ---------------- */
export const VERDICT = {
  PENDING: 'PENDING', // 미판정
  ACCEPTED: 'ACCEPTED', // 검토 반영
  REJECTED: 'REJECTED', // 오류 아님
  // HOLD: 'HOLD',      // 보류 — 기획 확정 시 활성화
}

export const VERDICT_LABEL = {
  [VERDICT.PENDING]: '미판정',
  [VERDICT.ACCEPTED]: '검토 반영',
  [VERDICT.REJECTED]: '오류 아님',
}

/* ---------------- 목록 필터 ---------------- */
export const DOC_SORT = {
  UPDATED_DESC: 'UPDATED_DESC',
  NAME_ASC: 'NAME_ASC',
}

export const DOC_SORT_LABEL = {
  [DOC_SORT.UPDATED_DESC]: '최근 수정',
  [DOC_SORT.NAME_ASC]: '이름순',
}

export const DOC_PERIOD = {
  ALL: 'ALL',
  D7: 'D7',
  D30: 'D30',
  D90: 'D90',
}

export const DOC_PERIOD_LABEL = {
  [DOC_PERIOD.ALL]: '전체 기간',
  [DOC_PERIOD.D7]: '최근 7일',
  [DOC_PERIOD.D30]: '최근 30일',
  [DOC_PERIOD.D90]: '최근 90일',
}

export const FINDING_SORT = {
  CONFIDENCE_DESC: 'CONFIDENCE_DESC',
  PAGE_ASC: 'PAGE_ASC',
}

export const FINDING_SORT_LABEL = {
  [FINDING_SORT.CONFIDENCE_DESC]: '확신도순',
  [FINDING_SORT.PAGE_ASC]: '페이지순',
}

/* ---------------- 업로드 제약 (와이어프레임 UP-01) ---------------- */
export const UPLOAD_LIMIT = {
  ACCEPT: 'application/pdf',
  MAX_SIZE_MB: 50,
  MAX_PAGES: 200,
}
