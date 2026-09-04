import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { reviewApi } from '@/api'
import {
  FINDING_SORT,
  FINDING_TYPE_ORDER,
  JOB_STATUS,
  VERDICT,
} from '@/constants/enums'

const LAYOUT_KEY = 'bizxray.review.layout'
const OUTLINE_RANGE = { min: 180, max: 420, default: 248 }
const PANEL_RANGE = { min: 300, max: 620, default: 400 }

const clamp = (value, { min, max }) => Math.min(max, Math.max(min, Math.round(value)))

function loadLayout() {
  try {
    const saved = JSON.parse(localStorage.getItem(LAYOUT_KEY) || '{}')
    return {
      outlineWidth: clamp(saved.outlineWidth ?? OUTLINE_RANGE.default, OUTLINE_RANGE),
      panelWidth: clamp(saved.panelWidth ?? PANEL_RANGE.default, PANEL_RANGE),
    }
  } catch {
    return { outlineWidth: OUTLINE_RANGE.default, panelWidth: PANEL_RANGE.default }
  }
}

/**
 * 검토 화면 상태 (REV-01 ~ REV-07)
 * - 원문 ↔ 검토 항목 양방향 앵커의 단일 소스입니다.
 * - 좌 · 우 사이드바 너비도 여기서 관리하고 localStorage 에 저장합니다.
 */
export const useReviewStore = defineStore('review', () => {
  const job = ref(null)
  /** 현재 검토 중인 문서 (제목 · 쪽수) — 챗봇 컨텍스트에도 씁니다. */
  const document = ref(null)
  const sections = ref([])
  const pages = ref([])
  const findings = ref([])
  const annotations = ref([])
  const loading = ref(false)
  const error = ref('')

  /* 레이아웃 (드래그로 조절, 새로고침 후에도 유지) */
  const layout = loadLayout()
  const outlineWidth = ref(layout.outlineWidth)
  const panelWidth = ref(layout.panelWidth)

  function persistLayout() {
    try {
      localStorage.setItem(
        LAYOUT_KEY,
        JSON.stringify({ outlineWidth: outlineWidth.value, panelWidth: panelWidth.value }),
      )
    } catch {
      /* 저장 실패는 무시 */
    }
  }

  function setOutlineWidth(value) {
    outlineWidth.value = clamp(value, OUTLINE_RANGE)
  }

  function setPanelWidth(value) {
    panelWidth.value = clamp(value, PANEL_RANGE)
  }

  function resetLayout() {
    outlineWidth.value = OUTLINE_RANGE.default
    panelWidth.value = PANEL_RANGE.default
    persistLayout()
  }

  /* 뷰어 상태 */
  const currentPage = ref(1)
  const zoom = ref(82)
  const showEvidence = ref(true)

  /* 목록 상태 */
  const activeFindingId = ref(null)
  const panelTab = ref('findings')
  const filterType = ref('ALL')
  const sort = ref(FINDING_SORT.CONFIDENCE_DESC)
  const undecidedOnly = ref(false)
  const flashAnchorId = ref(null)

  /* ---------------- computed ---------------- */
  const activeFinding = computed(
    () => findings.value.find((f) => f.id === activeFindingId.value) || null,
  )

  const countsByType = computed(() =>
    FINDING_TYPE_ORDER.reduce((acc, type) => {
      acc[type] = findings.value.filter((f) => f.type === type).length
      return acc
    }, {}),
  )

  const decidedCount = computed(
    () => findings.value.filter((f) => f.verdict !== VERDICT.PENDING).length,
  )

  const visibleFindings = computed(() => {
    let list = [...findings.value]
    if (filterType.value !== 'ALL') list = list.filter((f) => f.type === filterType.value)
    if (undecidedOnly.value) list = list.filter((f) => f.verdict === VERDICT.PENDING)
    list.sort((a, b) =>
      sort.value === FINDING_SORT.PAGE_ASC
        ? a.page - b.page || b.confidence - a.confidence
        : b.confidence - a.confidence || a.page - b.page,
    )
    return list
  })

  /** anchorId -> finding (원문 하이라이트 클릭용) */
  const findingByAnchor = computed(() => {
    const map = {}
    findings.value
      .filter((finding) => finding.verdict !== VERDICT.REJECTED)
      .forEach((finding) => {
        finding.evidence?.forEach((evidence) => {
          map[evidence.anchorId] = finding
        })
      })
    return map
  })

  const currentPageData = computed(
    () => pages.value.find((p) => p.page === currentPage.value) || null,
  )

  /* ---------------- actions ---------------- */
  async function load(jobId) {
    loading.value = true
    error.value = ''
    try {
      const [jobData, sectionData, pageData, findingData] = await Promise.all([
        reviewApi.getJob(jobId),
        reviewApi.getSections(jobId),
        reviewApi.getPages(jobId),
        reviewApi.getFindings(jobId),
      ])
      job.value = jobData
      sections.value = sectionData
      pages.value = pageData
      findings.value = findingData
      panelTab.value = 'findings'
      try {
        annotations.value = JSON.parse(localStorage.getItem(`bizxray.annotations.${jobId}`) || '[]')
      } catch {
        annotations.value = []
      }
      activeFindingId.value = null
      currentPage.value = findingData[0]?.page ?? 1
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  /** 분석 진행 화면용 폴링 */
  async function pollJob(jobId, { onTick } = {}) {
    const tick = async () => {
      const data = await reviewApi.getJob(jobId)
      job.value = data
      onTick?.(data)
      return data
    }
    let data = await tick()
    while (data.status !== JOB_STATUS.DONE && data.status !== JOB_STATUS.FAILED) {
      await new Promise((resolve) => setTimeout(resolve, 400))
      data = await tick()
    }
    return data
  }

  function goToPage(page) {
    const max = pages.value.length || 1
    currentPage.value = Math.min(max, Math.max(1, Number(page) || 1))
  }

  /** 검토 항목 → 원문 (REV-06) */
  function selectFinding(findingId, evidenceIndex = 0) {
    const finding = findings.value.find((f) => f.id === findingId)
    if (!finding) return
    panelTab.value = 'findings'
    activeFindingId.value = findingId
    const evidence = finding.evidence?.[evidenceIndex]
    if (evidence) {
      goToPage(evidence.page)
      flashAnchorId.value = evidence.anchorId
      setTimeout(() => (flashAnchorId.value = null), 900)
    }
  }

  /** 원문 하이라이트 → 검토 항목 (REV-06) */
  function selectAnchor(anchorId) {
    const finding = findingByAnchor.value[anchorId]
    if (finding) {
      panelTab.value = 'findings'
      activeFindingId.value = finding.id
    }
  }

  function toggleTypeFilter(type) {
    filterType.value = filterType.value === type ? 'ALL' : type
  }

  async function setVerdict(findingId, verdict) {
    const finding = findings.value.find((f) => f.id === findingId)
    if (!finding || !job.value) return
    const previous = finding.verdict
    finding.verdict = verdict // 낙관적 업데이트
    try {
      const updated = await reviewApi.updateVerdict(job.value.id, findingId, verdict)
      Object.assign(finding, updated)
    } catch (e) {
      finding.verdict = previous
      throw e
    }
  }

  async function addFinding(draft) {
    if (!job.value) return null
    const created = await reviewApi.promoteToFinding(job.value.id, draft)
    findings.value.push(created)
    panelTab.value = 'findings'
    activeFindingId.value = created.id
    return created
  }

  function persistAnnotations() {
    if (!job.value) return
    try {
      localStorage.setItem(`bizxray.annotations.${job.value.id}`, JSON.stringify(annotations.value))
    } catch {
      /* 저장 실패 시 현재 세션에서는 유지합니다. */
    }
  }

  function addAnnotation({ page, anchorId, selectedText = '', context = '', findingId = null, text }) {
    if (!job.value) return null
    const annotation = {
      id: `annotation-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      page,
      anchorId,
      selectedText,
      context,
      findingId,
      text,
      createdAt: new Date().toISOString(),
    }
    annotations.value.push(annotation)
    persistAnnotations()
    return annotation
  }

  function updateAnnotation(annotationId, text) {
    const annotation = annotations.value.find((item) => item.id === annotationId)
    if (!annotation || !text.trim()) return null
    annotation.text = text.trim()
    annotation.updatedAt = new Date().toISOString()
    persistAnnotations()
    return annotation
  }

  function removeAnnotation(annotationId) {
    const index = annotations.value.findIndex((item) => item.id === annotationId)
    if (index < 0) return false
    annotations.value.splice(index, 1)
    persistAnnotations()
    return true
  }

  function selectAnnotation(annotationId) {
    const annotation = annotations.value.find((item) => item.id === annotationId)
    if (!annotation) return
    panelTab.value = 'annotations'
    activeFindingId.value = null
    goToPage(annotation.page)
    flashAnchorId.value = annotation.anchorId
    setTimeout(() => (flashAnchorId.value = null), 900)
  }

  const undoVerdict = (findingId) => setVerdict(findingId, VERDICT.PENDING)

  async function complete() {
    if (!job.value) return null
    const result = await reviewApi.complete(job.value.id)
    job.value = result
    return result
  }

  function reset() {
    job.value = null
    document.value = null
    sections.value = []
    pages.value = []
    findings.value = []
    annotations.value = []
    activeFindingId.value = null
    panelTab.value = 'findings'
    filterType.value = 'ALL'
    undecidedOnly.value = false
    currentPage.value = 1
  }

  return {
    job,
    document,
    sections,
    pages,
    findings,
    annotations,
    loading,
    error,
    outlineWidth,
    panelWidth,
    setOutlineWidth,
    setPanelWidth,
    persistLayout,
    resetLayout,
    currentPage,
    zoom,
    showEvidence,
    activeFindingId,
    panelTab,
    filterType,
    sort,
    undecidedOnly,
    flashAnchorId,
    activeFinding,
    countsByType,
    decidedCount,
    visibleFindings,
    findingByAnchor,
    currentPageData,
    load,
    pollJob,
    goToPage,
    selectFinding,
    selectAnchor,
    toggleTypeFilter,
    addFinding,
    addAnnotation,
    updateAnnotation,
    removeAnnotation,
    selectAnnotation,
    setVerdict,
    undoVerdict,
    complete,
    reset,
  }
})
