/**
 * HTTP 클라이언트 (fetch 래퍼)
 * - baseURL / 인증 헤더 / 에러 정규화 / 업로드 진행률을 한 곳에서 처리합니다.
 * - 화면과 스토어는 이 파일을 직접 쓰지 않고 api/modules/* 를 통해 호출합니다.
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const TOKEN_KEY = 'logicheck.token'

export class ApiError extends Error {
  constructor(status, message, code = null, details = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

function buildUrl(path, params) {
  const url = `${BASE_URL}${path}`
  if (!params) return url
  const query = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&')
  return query ? `${url}?${query}` : url
}

async function parseBody(response) {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function request(method, path, { params, body, headers, signal } = {}) {
  const token = tokenStore.get()
  const isFormData = body instanceof FormData

  const init = {
    method,
    signal,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  }
  if (body !== undefined && method !== 'GET' && method !== 'HEAD') {
    init.body = isFormData ? body : JSON.stringify(body)
  }

  let response
  try {
    response = await fetch(buildUrl(path, params), init)
  } catch {
    throw new ApiError(0, '서버에 연결할 수 없습니다. 네트워크 상태를 확인해주세요.')
  }

  const data = await parseBody(response)

  if (!response.ok) {
    const message =
      (data && (data.message || data.error)) ||
      (response.status === 401
        ? '로그인이 필요합니다.'
        : response.status === 403
          ? '권한이 없습니다.'
          : response.status === 404
            ? '요청한 리소스를 찾을 수 없습니다.'
            : '요청을 처리하지 못했습니다.')
    throw new ApiError(response.status, message, data?.code ?? null, data ?? null)
  }

  return data
}

/** 업로드 진행률이 필요해 XHR 사용 */
export function upload(path, formData, { onProgress } = {}) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', buildUrl(path))
    const token = tokenStore.get()
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
    xhr.onload = () => {
      let data
      try {
        data = xhr.responseText ? JSON.parse(xhr.responseText) : null
      } catch {
        data = xhr.responseText
      }
      if (xhr.status >= 200 && xhr.status < 300) resolve(data)
      else reject(new ApiError(xhr.status, data?.message || '업로드에 실패했습니다.', data?.code ?? null))
    }
    xhr.onerror = () => reject(new ApiError(0, '업로드 중 네트워크 오류가 발생했습니다.'))
    xhr.send(formData)
  })
}

export const http = {
  get: (path, options) => request('GET', path, options),
  post: (path, body, options) => request('POST', path, { ...options, body }),
  put: (path, body, options) => request('PUT', path, { ...options, body }),
  patch: (path, body, options) => request('PATCH', path, { ...options, body }),
  delete: (path, options) => request('DELETE', path, options),
  upload,
}

export default http
