/**
 * API 경로 정의 — BE 명세가 확정되면 이 파일만 수정합니다.
 * (VITE_API_BASE_URL 이 앞에 붙습니다. 기본값 '/api')
 */
export const EP = {
  auth: {
    signup: '/auth/signup',
    login: '/auth/login',
    logout: '/auth/logout',
    me: '/auth/me',
    password: '/auth/me/password',
    withdraw: '/auth/me',
  },
  tags: {
    list: '/tags',
  },
  documents: {
    list: '/documents',
    detail: (id) => `/documents/${id}`,
    upload: '/documents',
    rename: (id) => `/documents/${id}/name`,
    remove: (id) => `/documents/${id}`,
  },
  reviews: {
    createJob: '/review-jobs',
    job: (jobId) => `/review-jobs/${jobId}`,
    jobByDocument: (documentId) => `/documents/${documentId}/review-jobs/latest`,
    sections: (jobId) => `/review-jobs/${jobId}/sections`,
    findings: (jobId) => `/review-jobs/${jobId}/findings`,
    verdict: (jobId, findingId) => `/review-jobs/${jobId}/findings/${findingId}/verdict`,
    complete: (jobId) => `/review-jobs/${jobId}/complete`,
    report: (jobId) => `/review-jobs/${jobId}/report`,
    ask: (jobId) => `/review-jobs/${jobId}/questions`,
  },
}

export default EP
