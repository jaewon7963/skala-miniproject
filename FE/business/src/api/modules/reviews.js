import http from '@/api/http'
import EP from '@/api/endpoints'
import { USE_MOCK } from '@/api/config'
import mock from '@/api/mock/handlers'

/**
 * 분석 · 검토 API (UP-02, ANL-01~03, REV-01~07, SHR-01/03)
 *
 * 응답 계약(핵심)
 *  Job      { id, documentId, phase, status, parseProgress, analyzeProgress,
 *             steps[], partialFailures[], discovered[], summary }
 *  Section  { id, title, level, page, findingCount }
 *  Finding  { id, type, page, sectionId, title, description, confidence,
 *             method, verdict, evidence: [{ anchorId, page, label }] }
 */
export const reviewApi = {
  createJob: (documentId) =>
    USE_MOCK ? mock.reviews.createJob(documentId) : http.post(EP.reviews.createJob, { documentId }),
  getJob: (jobId) => (USE_MOCK ? mock.reviews.getJob(jobId) : http.get(EP.reviews.job(jobId))),
  getLatestJobByDocument: (documentId) =>
    USE_MOCK
      ? mock.reviews.getLatestJobByDocument(documentId)
      : http.get(EP.reviews.jobByDocument(documentId)),

  getSections: (jobId) =>
    USE_MOCK ? mock.reviews.getSections(jobId) : http.get(EP.reviews.sections(jobId)),
  /** 원문 뷰어 데이터. 실제 서비스에서는 PDF 렌더러 + bbox 로 대체됩니다. */
  getPages: (jobId) =>
    USE_MOCK ? mock.reviews.getPages(jobId) : http.get(`${EP.reviews.job(jobId)}/pages`),
  getFindings: (jobId, params) =>
    USE_MOCK ? mock.reviews.getFindings(jobId, params) : http.get(EP.reviews.findings(jobId), { params }),

  updateVerdict: (jobId, findingId, verdict) =>
    USE_MOCK
      ? mock.reviews.updateVerdict(jobId, findingId, verdict)
      : http.patch(EP.reviews.verdict(jobId, findingId), { verdict }),

  complete: (jobId) =>
    USE_MOCK ? mock.reviews.complete(jobId) : http.post(EP.reviews.complete(jobId)),
  getReport: (jobId) =>
    USE_MOCK ? mock.reviews.getReport(jobId) : http.get(EP.reviews.report(jobId)),

  ask: (jobId, payload) =>
    USE_MOCK ? mock.reviews.ask(jobId, payload) : http.post(EP.reviews.ask(jobId), payload),
  promoteToFinding: (jobId, draft) =>
    USE_MOCK
      ? mock.reviews.promoteToFinding(jobId, draft)
      : http.post(EP.reviews.findings(jobId), draft),
}

export default reviewApi
