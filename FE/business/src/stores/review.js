import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { reviewApi } from '@/api'
import {
  FINDING_SORT,
  FINDING_TYPE_ORDER,
  JOB_STATUS,
  VERDICT,
} from '@/constants/enums'

/**
 * 검토 화면 상태 (REV-01 ~ REV-07)
 * - 원문 ↔ 검토 항목 양방향 앵커의 단일 소스입니다.
 */
export const useReviewStore = defineStore('review', () => {
  const job = ref(null)
  const sections = ref([])
  const pages = ref([])
  const findings = ref([])
  const loading = ref(false)
  const error = ref('')

  /* 뷰어 상태 */
  const currentPage = ref(1)
  const zoom = ref(82)
  const showEvidence = ref(true)

  /* 목록 상태 */
  const activeFindingId = ref(null)
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
    findings.value.forEach((finding) => {
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
    if (finding) activeFindingId.value = finding.id
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

  const undoVerdict = (findingId) => setVerdict(findingId, VERDICT.PENDING)

  async function complete() {
    if (!job.value) return null
    const result = await reviewApi.complete(job.value.id)
    job.value = result
    return result
  }

  function reset() {
    job.value = null
    sections.value = []
    pages.value = []
    findings.value = []
    activeFindingId.value = null
    filterType.value = 'ALL'
    undecidedOnly.value = false
    currentPage.value = 1
  }

  return {
    job,
    sections,
    pages,
    findings,
    loading,
    error,
    currentPage,
    zoom,
    showEvidence,
    activeFindingId,
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
    setVerdict,
    undoVerdict,
    complete,
    reset,
  }
})
