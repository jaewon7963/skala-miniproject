import http from '@/api/http'
import EP from '@/api/endpoints'
import { USE_MOCK } from '@/api/config'
import mock from '@/api/mock/handlers'

/**
 * 문서 API (DASH-01 ~ DASH-04, UP-01)
 * list 응답 계약
 *  { items: Document[], total, page, size, counts: { ALL, REVIEWING, DONE, FAILED } }
 */
export const documentApi = {
  list: (params) => (USE_MOCK ? mock.documents.list(params) : http.get(EP.documents.list, { params })),
  get: (id) => (USE_MOCK ? mock.documents.get(id) : http.get(EP.documents.detail(id))),
  tags: () => (USE_MOCK ? mock.documents.tags() : http.get(EP.tags.list)),

  upload: (file, options) => {
    if (USE_MOCK) return mock.documents.upload(file, options)
    const form = new FormData()
    form.append('file', file)
    return http.upload(EP.documents.upload, form, options)
  },

  rename: (id, name) =>
    USE_MOCK ? mock.documents.rename(id, name) : http.patch(EP.documents.rename(id), { name }),
  remove: (id) => (USE_MOCK ? mock.documents.remove(id) : http.delete(EP.documents.remove(id))),
}

export default documentApi
