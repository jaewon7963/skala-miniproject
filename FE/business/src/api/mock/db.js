/**
 * Mock 데이터 저장소 (메모리)
 * 실제 AI/BE 응답과 "동일한 JSON 구조"를 유지하는 것이 목적입니다.
 * BE 연동 후에는 이 폴더 전체가 삭제 대상입니다.
 */
import {
  DOC_STATUS,
  FINDING_METHOD,
  FINDING_TYPE,
  VERDICT,
} from '@/constants/enums'

export const mockUser = {
  id: 'u-1',
  name: '김평가',
  email: 'kim@company.com',
  organization: 'company.com',
  createdAt: '2026-08-01T09:00:00+09:00',
}

export const mockFolders = [
  { id: 'fd-1', name: '2026 창업지원패키지', count: 2 },
  { id: 'fd-2', name: '시리즈A 실사', count: 1 },
]

export const mockDocuments = [
  {
    id: 'doc-1',
    name: 'AI 매장 안내 로봇 사업계획서',
    version: 3,
    pageCount: 21,
    sizeBytes: 4.2 * 1024 * 1024,
    status: DOC_STATUS.REVIEWING,
    folderId: 'fd-1',
    latestJobId: 'job-1',
    updatedAt: '2026-09-02T14:20:00+09:00',
    summary: '무인매장 안내 로봇 개발 및 사업화 계획',
  },
  {
    id: 'doc-2',
    name: '스마트팩토리 R&D 과제제안서',
    version: 2,
    pageCount: 34,
    sizeBytes: 8.1 * 1024 * 1024,
    status: DOC_STATUS.FAILED,
    folderId: 'fd-1',
    latestJobId: null,
    updatedAt: '2026-09-01T09:10:00+09:00',
    summary: '스캔 PDF · 텍스트 레이어 없음',
  },
  {
    id: 'doc-3',
    name: '친환경 포장재 시리즈A IR',
    version: 1,
    pageCount: 18,
    sizeBytes: 2.6 * 1024 * 1024,
    status: DOC_STATUS.DONE,
    folderId: 'fd-2',
    latestJobId: 'job-3',
    updatedAt: '2026-08-30T18:44:00+09:00',
    summary: '시리즈A 투자 유치용 IR 자료',
  },
  {
    id: 'doc-4',
    name: '바이오 헬스케어 정부과제 계획서',
    version: 1,
    pageCount: 27,
    sizeBytes: 5.4 * 1024 * 1024,
    status: DOC_STATUS.DONE,
    folderId: null,
    latestJobId: 'job-4',
    updatedAt: '2026-08-28T11:02:00+09:00',
    summary: '디지털 치료제 임상 실증 과제',
  },
]

/** 좌측 목차 (REV-02) */
export const mockSections = [
  { id: 'sec-1', title: '사업 개요', level: 1, page: 1 },
  { id: 'sec-2', title: '시장 분석', level: 1, page: 3 },
  { id: 'sec-2-1', title: '목표 시장 규모', level: 2, page: 4 },
  { id: 'sec-2-2', title: '경쟁사 비교', level: 2, page: 5 },
  { id: 'sec-3', title: '제품 · 기술', level: 1, page: 7 },
  { id: 'sec-3-1', title: '핵심 기술 구성', level: 2, page: 8 },
  { id: 'sec-4', title: '사업화 전략', level: 1, page: 10 },
  { id: 'sec-5', title: '매출 · 재무 계획', level: 1, page: 11 },
  { id: 'sec-6', title: 'KPI 및 성과 지표', level: 1, page: 15 },
  { id: 'sec-7', title: '추진 일정', level: 1, page: 18 },
  { id: 'sec-8', title: '예산 집행 계획', level: 1, page: 19 },
]

/** 분석 파이프라인 단계 (UP-02 / ANL-01) */
export const mockJobSteps = [
  { key: 'validate', label: '파일 검증', detail: 'MIME · 용량 · 암호화 여부 확인' },
  { key: 'split', label: '페이지 분할 · 텍스트 레이어 추출', detail: 'bbox 좌표와 함께 저장' },
  { key: 'outline', label: '목차 · 섹션 인식', detail: '제목 계층 복원' },
  { key: 'extract', label: '표 · 수치 · 주장 · 기술 · KPI 추출', detail: '요소 단위로 페이지 · 위치 태깅' },
  { key: 'persist', label: '구조화 저장', detail: 'Section / Page / ExtractedElement 커밋' },
]

/**
 * 검토 결과(Finding)
 * evidence[].anchorId 는 원문 뷰어의 블록 id 와 연결됩니다. (REV-06 양방향 앵커)
 */
export const mockFindings = [
  {
    id: 'fd-01',
    type: FINDING_TYPE.ERROR,
    page: 11,
    sectionId: 'sec-5',
    title: '매출 합계와 표 5-1 소계가 불일치합니다',
    description:
      "본문 '2027년 매출 24억 원'과 표 5-1 합계 36.8억 원의 연도별 값이 서로 맞지 않습니다 · 차이 3,200만 원",
    confidence: 0.96,
    method: FINDING_METHOD.DETERMINISTIC,
    verdict: VERDICT.PENDING,
    evidence: [
      { anchorId: 'b-11-2', page: 11, label: '본문 문단 · 2027년 매출 24억 원' },
      { anchorId: 'b-11-3', page: 11, label: '표 5-1 합계 행' },
    ],
  },
  {
    id: 'fd-02',
    type: FINDING_TYPE.ERROR,
    page: 7,
    sectionId: 'sec-3',
    title: '인건비 단가 × 인원 계산값이 다릅니다',
    description: '표 3-1의 소계가 단가 × 인원 재계산 결과와 4백만 원 차이가 납니다.',
    confidence: 0.93,
    method: FINDING_METHOD.DETERMINISTIC,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-7-2', page: 7, label: '표 3-1 인건비 소계' }],
  },
  {
    id: 'fd-03',
    type: FINDING_TYPE.ERROR,
    page: 9,
    sectionId: 'sec-4',
    title: '매장 수 전제가 서로 다릅니다',
    description:
      'p.9 시장 진입 계획은 2027년 40개 매장을 전제하지만, p.11 매출 추정은 같은 해 62개 매장을 가정합니다.',
    confidence: 0.89,
    method: FINDING_METHOD.DETERMINISTIC,
    verdict: VERDICT.PENDING,
    evidence: [
      { anchorId: 'b-9-2', page: 9, label: '2027년 3개 지역 40개 매장 확보' },
      { anchorId: 'b-11-4', page: 11, label: '62개 매장 × 연 3,870만 원' },
    ],
  },
  {
    id: 'fd-04',
    type: FINDING_TYPE.NEEDS_CHECK,
    page: 11,
    sectionId: 'sec-5',
    title: 'CAGR 34%의 산출 근거를 찾지 못했습니다',
    description: '문서 내에서 34%를 뒷받침하는 시장 자료 또는 계산 과정이 확인되지 않습니다.',
    confidence: 0.78,
    method: FINDING_METHOD.RAG,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-11-5', page: 11, label: '본문 문단 · CAGR 34% 전제' }],
  },
  {
    id: 'fd-05',
    type: FINDING_TYPE.NEEDS_CHECK,
    page: 5,
    sectionId: 'sec-2-2',
    title: '목표 고객과 판매 채널이 연결되지 않습니다',
    description:
      '최종 사용자는 개인 고객으로 정의했으나 판매 계획은 기업 직접 영업만 제시하고 있습니다.',
    confidence: 0.74,
    method: FINDING_METHOD.RAG,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-5-2', page: 5, label: '목표 고객 · 판매 채널 문단' }],
  },
  {
    id: 'fd-06',
    type: FINDING_TYPE.NEEDS_CHECK,
    page: 8,
    sectionId: 'sec-3-1',
    title: '기술 구성이 응답시간 목표를 만족하는지 확인이 필요합니다',
    description: '0.5초 응답 목표에 비해 대형 비전 모델 3종을 순차 실행하며 처리량 근거가 없습니다.',
    confidence: 0.69,
    method: FINDING_METHOD.RAG,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-8-2', page: 8, label: '모델 3종 순차 처리 구성도' }],
  },
  {
    id: 'fd-07',
    type: FINDING_TYPE.NO_EVIDENCE,
    page: 15,
    sectionId: 'sec-6',
    title: "KPI '재방문율 20%'의 측정 방법이 없습니다",
    description: '지표는 제시되었으나 측정 주기 · 산식 · 데이터 출처가 기재되지 않았습니다.',
    confidence: 0.71,
    method: FINDING_METHOD.RAG,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-15-2', page: 15, label: '핵심 KPI 표 · 재방문율 20%' }],
  },
  {
    id: 'fd-08',
    type: FINDING_TYPE.NO_EVIDENCE,
    page: 4,
    sectionId: 'sec-2-1',
    title: '목표 시장 규모의 출처가 없습니다',
    description: '2027년 국내 시장 규모 1.2조 원의 인용 출처와 조사 기관이 확인되지 않습니다.',
    confidence: 0.66,
    method: FINDING_METHOD.RAG,
    verdict: VERDICT.PENDING,
    evidence: [{ anchorId: 'b-4-2', page: 4, label: '시장 규모 문단' }],
  },
]

/** 원문 뷰어용 페이지 데이터 (실제로는 파싱 결과 + bbox 가 내려옵니다) */
const detailedPages = {
  4: [
    { id: 'b-4-1', kind: 'h2', text: '2.1 목표 시장 규모' },
    {
      id: 'b-4-2',
      kind: 'p',
      text: '2027년 국내 무인매장 안내 로봇 시장은 1.2조 원 규모로 성장할 것으로 전망된다.',
    },
    { id: 'b-4-3', kind: 'p', text: '초기 목표 시장은 수도권 대형 프랜차이즈 매장으로 설정한다.' },
  ],
  5: [
    { id: 'b-5-1', kind: 'h2', text: '2.2 경쟁사 비교' },
    {
      id: 'b-5-2',
      kind: 'p',
      text: '목표 고객은 20~30대 개인 사용자이며, 판매는 대기업 구매팀 직접 영업으로 진행한다.',
    },
  ],
  7: [
    { id: 'b-7-1', kind: 'h2', text: '3. 제품 · 기술' },
    {
      id: 'b-7-2',
      kind: 'table',
      caption: '[표 3-1] 인건비 산출',
      head: ['구분', '인원', '월 단가', '기간', '소계'],
      rows: [
        ['연구개발', '5명', '450만', '8개월', '1억 8,000만'],
        ['시스템', '2명', '400만', '6개월', '4,800만'],
        ['합계', '7명', '-', '-', '2억 3,200만'],
      ],
    },
  ],
  8: [
    { id: 'b-8-1', kind: 'h2', text: '3.1 핵심 기술 구성' },
    {
      id: 'b-8-2',
      kind: 'figure',
      text: '카메라 입력 → 비전 AI 모델 3종 순차 처리 → 행동 분석 → 로봇 안내 (0.5초 이내)',
    },
  ],
  9: [
    { id: 'b-9-1', kind: 'h2', text: '4. 사업화 전략' },
    { id: 'b-9-2', kind: 'p', text: '2027년까지 3개 지역에 40개 매장을 확보한다.' },
  ],
  11: [
    { id: 'b-11-1', kind: 'h2', text: '5. 매출 · 재무 계획' },
    {
      id: 'b-11-2',
      kind: 'p',
      text: '2027년 매출 24억 원, 영업이익률 18% 달성 (표 5-1 참조)',
    },
    {
      id: 'b-11-3',
      kind: 'table',
      caption: '[표 5-1] 연도별 매출 추정',
      head: ['구분', '2025', '2026', '2027', '합계'],
      rows: [
        ['매출', '3.2억', '9.6억', '24억', '36.8억'],
        ['원가', '2.1억', '5.8억', '13.4억', '21.3억'],
        ['영업이익', '1.1억', '3.8억', '10.6억', '15.5억'],
      ],
    },
    { id: 'b-11-4', kind: 'p', text: '2027년 매출은 62개 매장 × 연 3,870만 원으로 산출하였다.' },
    { id: 'b-11-5', kind: 'p', text: '연평균 성장률(CAGR) 34%를 전제로 산출하였다.' },
  ],
  15: [
    { id: 'b-15-1', kind: 'h2', text: '6. KPI 및 성과 지표' },
    {
      id: 'b-15-2',
      kind: 'table',
      caption: '[표 6-1] 핵심 KPI',
      head: ['지표', '현재', '목표', '측정 방법'],
      rows: [
        ['객체 인식 정확도', '82%', '95%', '자체 시험'],
        ['평균 응답 시간', '2.1초', '0.5초', '자체 시험'],
        ['재방문율', '-', '20%', '-'],
      ],
    },
  ],
}

export function buildPages(pageCount = 21) {
  return Array.from({ length: pageCount }, (_, index) => {
    const page = index + 1
    const section = [...mockSections].reverse().find((s) => page >= s.page) || mockSections[0]
    return {
      page,
      sectionId: section.id,
      sectionTitle: section.title,
      blocks: detailedPages[page] || [
        { id: `b-${page}-1`, kind: 'h2', text: section.title },
        {
          id: `b-${page}-2`,
          kind: 'p',
          text: `${section.title} 관련 본문이 표시되는 영역입니다. 실제 서비스에서는 파싱된 텍스트와 bbox 좌표가 이 위치에 렌더링됩니다.`,
        },
        { id: `b-${page}-3`, kind: 'figure', text: '그래프 · 이미지 영역' },
      ],
    }
  })
}
