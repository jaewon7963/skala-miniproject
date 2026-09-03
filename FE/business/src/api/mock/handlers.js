/**
 * Mock 핸들러 — api/modules/* 와 시그니처 · 응답 구조가 동일합니다.
 * BE 연동 시 VITE_USE_MOCK=false 로 두면 이 파일은 호출되지 않습니다.
 */
import { ApiError } from '@/api/http'
import {
  DOC_STATUS,
  FINDING_TYPE,
  FINDING_TYPE_ORDER,
  JOB_STATUS,
  UPLOAD_LIMIT,
  VERDICT,
} from '@/constants/enums'
import {
  buildPages,
  mockDocuments,
  mockFindings,
  mockTags,
  mockJobSteps,
  mockSections,
  mockUser,
} from './db'

const LATENCY = Number(import.meta.env.VITE_MOCK_LATENCY ?? 300)
const delay = (ms = LATENCY) => new Promise((resolve) => setTimeout(resolve, ms))
const clone = (value) => JSON.parse(JSON.stringify(value))

/* ------------------------------------------------------------------ */
/* 메모리 상태                                                          */
/* ------------------------------------------------------------------ */
const state = {
  user: clone(mockUser),
  documents: clone(mockDocuments),
  tags: clone(mockTags),
  jobs: {},
  findings: {},
  chats: {},
  seq: 100,
}

const PARSE_MS = 3600
const ANALYZE_MS = 4000

function seedJob(jobId, documentId, { done = false } = {}) {
  state.jobs[jobId] = {
    id: jobId,
    documentId,
    startedAt: done ? 0 : Date.now(),
    completedAt: done ? new Date().toISOString() : null,
    forceDone: done,
  }
  state.findings[jobId] = clone(mockFindings)
  return state.jobs[jobId]
}

seedJob('job-1', 'doc-1', { done: true })
seedJob('job-3', 'doc-3', { done: true })
seedJob('job-4', 'doc-4', { done: true })
// 데모용 판정 상태
// job-3 : 검토가 진행되다 만 문서 (앞 4건만 판정 · 나머지는 미판정으로 남겨 화면이 비어 보이지 않게)
// job-4 : 모든 항목을 판정한 완료 문서
state.findings['job-3'].forEach((f, i) => {
  if (i < 4) f.verdict = i % 2 === 0 ? VERDICT.ACCEPTED : VERDICT.REJECTED
})
state.findings['job-4'].forEach((f) => (f.verdict = VERDICT.ACCEPTED))

function nextId(prefix) {
  state.seq += 1
  return `${prefix}-${state.seq}`
}

function summarize(jobId) {
  const list = state.findings[jobId] || []
  const byType = FINDING_TYPE_ORDER.reduce((acc, type) => {
    acc[type] = list.filter((f) => f.type === type).length
    return acc
  }, {})
  return {
    total: list.length,
    byType,
    decided: list.filter((f) => f.verdict !== VERDICT.PENDING).length,
    accepted: list.filter((f) => f.verdict === VERDICT.ACCEPTED).length,
    rejected: list.filter((f) => f.verdict === VERDICT.REJECTED).length,
    // NOTE: 검토 점수 정책 미확정(기획서 7.10). 화면에서 숨길 수 있게 별도 필드로 둡니다.
    score: 72,
  }
}

/** 경과 시간 기반으로 파이프라인 진행 상태를 계산합니다. */
function buildJob(jobId) {
  const job = state.jobs[jobId]
  if (!job) throw new ApiError(404, '분석 작업을 찾을 수 없습니다.')

  const elapsed = job.forceDone ? PARSE_MS + ANALYZE_MS : Date.now() - job.startedAt
  const parseRatio = Math.min(1, elapsed / PARSE_MS)
  const analyzeRatio = Math.min(1, Math.max(0, (elapsed - PARSE_MS) / ANALYZE_MS))
  const stepIndex = Math.min(mockJobSteps.length - 1, Math.floor(parseRatio * mockJobSteps.length))

  const steps = mockJobSteps.map((step, index) => ({
    ...step,
    state: parseRatio >= 1 || index < stepIndex ? 'DONE' : index === stepIndex ? 'RUNNING' : 'WAIT',
  }))

  const all = state.findings[jobId] || []
  const discoveredCount = Math.floor(analyzeRatio * all.length)
  const done = analyzeRatio >= 1

  return {
    id: job.id,
    documentId: job.documentId,
    phase: parseRatio < 1 ? 'PARSE' : 'ANALYZE',
    status: done ? JOB_STATUS.DONE : JOB_STATUS.RUNNING,
    parseProgress: Math.round(parseRatio * 100),
    analyzeProgress: Math.round(analyzeRatio * 100),
    steps,
    partialFailures:
      parseRatio > 0.6
        ? [{ page: 14, reason: '이미지형 표는 텍스트 레이어가 없어 해당 구간을 제외하고 진행했습니다' }]
        : [],
    discovered: clone(all.slice(0, done ? all.length : discoveredCount)),
    summary: summarize(jobId),
    completedAt: done ? job.completedAt || new Date().toISOString() : null,
  }
}

function findDocument(id) {
  const doc = state.documents.find((d) => d.id === id)
  if (!doc) throw new ApiError(404, '문서를 찾을 수 없습니다.')
  return doc
}

/* ------------------------------------------------------------------ */
/* auth                                                                */
/* ------------------------------------------------------------------ */
export const auth = {
  async login({ email, password }) {
    await delay()
    if (!email || !password) throw new ApiError(400, '이메일과 비밀번호를 입력해주세요.')
    if (password.length < 8) throw new ApiError(401, '이메일 또는 비밀번호를 확인해주세요')
    return { token: 'mock-token', user: { ...state.user, email } }
  },
  async signup({ email }) {
    await delay()
    if (email === 'kim@company.com') throw new ApiError(409, '이미 가입된 이메일입니다')
    state.user = { ...state.user, email }
    return { token: 'mock-token', user: clone(state.user) }
  },
  async me() {
    await delay(120)
    return clone(state.user)
  },
  async changePassword({ currentPassword, newPassword }) {
    await delay()
    if (!currentPassword) throw new ApiError(400, '현재 비밀번호를 입력해주세요.')
    if (currentPassword === newPassword)
      throw new ApiError(400, '현재 비밀번호와 다른 비밀번호를 입력해주세요.')
    return { ok: true }
  },
  async logout() {
    await delay(120)
    return { ok: true }
  },
  async withdraw({ password }) {
    await delay()
    if (!password) throw new ApiError(400, '비밀번호를 다시 입력해주세요.')
    return { ok: true, purgeAfterDays: 30 }
  },
}

/* ------------------------------------------------------------------ */
/* documents                                                           */
/* ------------------------------------------------------------------ */
export const documents = {
  async list({ q = '', status = 'ALL', period = 'ALL', sort = 'UPDATED_DESC', tag = null, page = 1, size = 20 } = {}) {
    await delay()
    let items = clone(state.documents)

    if (q) {
      const keyword = q.trim().toLowerCase()
      items = items.filter(
        (d) =>
          d.name.toLowerCase().includes(keyword) ||
          (d.summary || '').toLowerCase().includes(keyword) ||
          (d.tags || []).some((tagId) => tagId.toLowerCase().includes(keyword)),
      )
    }
    if (status !== 'ALL') items = items.filter((d) => d.status === status)
    if (tag) items = items.filter((d) => d.tags?.includes(tag))
    if (period !== 'ALL') {
      const days = { D7: 7, D30: 30, D90: 90 }[period] ?? 3650
      const from = Date.now() - days * 864e5
      items = items.filter((d) => new Date(d.updatedAt).getTime() >= from)
    }

    items.sort((a, b) =>
      sort === 'NAME_ASC'
        ? a.name.localeCompare(b.name, 'ko')
        : new Date(b.updatedAt) - new Date(a.updatedAt),
    )

    const total = items.length
    const start = (page - 1) * size

    return {
      items: items.slice(start, start + size),
      total,
      page,
      size,
      // 상태별 집계 : 모든 상태를 내려주므로 상태별 합 = ALL 이 됩니다
      counts: Object.values(DOC_STATUS).reduce(
        (acc, status) => {
          acc[status] = state.documents.filter((d) => d.status === status).length
          return acc
        },
        { ALL: state.documents.length },
      ),
    }
  },

  async get(id) {
    await delay(150)
    return clone(findDocument(id))
  },

  async tags() {
    await delay(120)
    return state.tags.map((tag) => ({
      ...clone(tag),
      count: state.documents.filter((document) => document.tags?.includes(tag.id)).length,
    }))
  },

  async upload(file, { onProgress } = {}) {
    if (file.type !== UPLOAD_LIMIT.ACCEPT)
      throw new ApiError(415, 'PDF 파일만 업로드할 수 있습니다.')
    if (file.size > UPLOAD_LIMIT.MAX_SIZE_MB * 1024 * 1024)
      throw new ApiError(413, `최대 ${UPLOAD_LIMIT.MAX_SIZE_MB}MB 까지 업로드할 수 있습니다.`)

    for (let progress = 10; progress <= 100; progress += 15) {
      await delay(90)
      onProgress?.(Math.min(100, progress))
    }

    const doc = {
      id: nextId('doc'),
      name: file.name.replace(/\.pdf$/i, ''),
      version: 1,
      pageCount: 21,
      sizeBytes: file.size,
      status: DOC_STATUS.IDLE,
      tags: [],
      latestJobId: null,
      updatedAt: new Date().toISOString(),
      summary: '업로드 완료 · 분석 대기',
    }
    state.documents.unshift(doc)
    return clone(doc)
  },

  async rename(id, name) {
    await delay()
    const doc = findDocument(id)
    doc.name = name
    doc.updatedAt = new Date().toISOString()
    return clone(doc)
  },

  async remove(id) {
    await delay()
    findDocument(id)
    state.documents = state.documents.filter((d) => d.id !== id)
    return { ok: true }
  },
}

/* ------------------------------------------------------------------ */
/* reviews                                                             */
/* ------------------------------------------------------------------ */
export const reviews = {
  async createJob(documentId) {
    await delay(200)
    const doc = findDocument(documentId)
    const jobId = nextId('job')
    seedJob(jobId, documentId)
    doc.latestJobId = jobId
    doc.status = DOC_STATUS.PARSING
    return buildJob(jobId)
  },

  async getJob(jobId) {
    await delay(80)
    return buildJob(jobId)
  },

  async getLatestJobByDocument(documentId) {
    await delay(120)
    const doc = findDocument(documentId)
    if (!doc.latestJobId) throw new ApiError(404, '아직 분석하지 않은 문서입니다.')
    return buildJob(doc.latestJobId)
  },

  async getSections(jobId) {
    await delay(120)
    const list = state.findings[jobId] || []
    return mockSections.map((section) => ({
      ...section,
      findingCount: list.filter((f) => f.sectionId === section.id).length,
    }))
  },

  async getPages(jobId) {
    await delay(150)
    const job = state.jobs[jobId]
    const doc = job ? state.documents.find((d) => d.id === job.documentId) : null
    return buildPages(doc?.pageCount ?? 21)
  },

  async getFindings(jobId) {
    await delay(150)
    return clone(state.findings[jobId] || [])
  },

  async updateVerdict(jobId, findingId, verdict) {
    await delay(120)
    const finding = (state.findings[jobId] || []).find((f) => f.id === findingId)
    if (!finding) throw new ApiError(404, '검토 항목을 찾을 수 없습니다.')
    if (!Object.values(VERDICT).includes(verdict))
      throw new ApiError(400, '지원하지 않는 판정 값입니다.')
    finding.verdict = verdict
    finding.decidedAt = verdict === VERDICT.PENDING ? null : new Date().toISOString()
    return clone(finding)
  },

  async complete(jobId) {
    await delay()
    const job = state.jobs[jobId]
    if (!job) throw new ApiError(404, '분석 작업을 찾을 수 없습니다.')
    job.forceDone = true
    job.completedAt = new Date().toISOString()
    const doc = state.documents.find((d) => d.id === job.documentId)
    if (doc) {
      doc.status = DOC_STATUS.DONE
      doc.updatedAt = job.completedAt
    }
    return { ...buildJob(jobId), summary: summarize(jobId) }
  },

  async getReport(jobId) {
    await delay()
    const job = state.jobs[jobId]
    if (!job) throw new ApiError(404, '분석 작업을 찾을 수 없습니다.')
    const doc = state.documents.find((d) => d.id === job.documentId)
    const list = state.findings[jobId] || []
    return {
      jobId,
      documentId: doc?.id ?? null,
      documentName: doc?.name ?? '문서',
      documentVersion: doc?.version ?? null,
      reviewer: state.user.email,
      reviewedAt: new Date().toISOString(),
      verdict: 'CONDITIONAL', // 조건부 보완
      dueDate: '2026-09-12',
      receiver: '사업기획팀',
      summary: summarize(jobId),
      items: clone(
        list
          .filter((f) => f.verdict === VERDICT.ACCEPTED)
          .map((f, index) => ({
            no: index + 1,
            type: f.type,
            page: f.page,
            instruction: f.title,
          })),
      ),
      checklist: clone(
        list
          .filter((f) => f.verdict === VERDICT.ACCEPTED)
          .map((f) => `${f.title} 보완 자료 첨부`),
      ),
    }
  },

  async ask(jobId, { question, selection = null }) {
    await delay(600)
    const list = state.findings[jobId] || []
    const matched =
      list.find((f) => question.includes('매출') && f.type === FINDING_TYPE.ERROR) ||
      list.find((f) => question.includes('KPI') && f.type === FINDING_TYPE.NO_EVIDENCE)

    if (!matched) {
      return {
        answer: '이 문서 안에서 관련 근거를 찾지 못했습니다. 추측하지 않습니다.',
        evidences: [],
        promotable: false,
      }
    }
    return {
      answer: `${matched.description} (선택 원문: ${selection?.text ?? '문서 전체'})`,
      evidences: clone(matched.evidence),
      promotable: true,
      findingDraft: clone(matched),
    }
  },

  async promoteToFinding(jobId, draft) {
    await delay(200)
    const list = state.findings[jobId] || []
    const created = { ...clone(draft), id: nextId('fd'), verdict: VERDICT.PENDING }
    list.push(created)
    return created
  },
}

export default { auth, documents, reviews }
