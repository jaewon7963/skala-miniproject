/**
 * 문서명 변경 — 프론트 전용 저장소
 *
 * 백엔드 rename API가 준비되기 전까지 변경한 문서명을 localStorage 에 보관하고,
 * 목록 · 상세 응답에 덮어씌워 화면에 반영합니다.
 *
 * BE 연동 시 정리 방법
 *  1) stores/documents.js 의 rename() 에서 documentApi.rename() 을 호출하도록 되돌리고
 *  2) applyName / applyNames 호출과 이 파일을 삭제하면 됩니다.
 */
const STORAGE_KEY = 'bizxray.document.names'

function load() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

let overrides = load()

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(overrides))
  } catch {
    /* 저장에 실패해도 현재 세션에서는 유지됩니다 */
  }
}

/** 변경된 이름이 있으면 덮어쓴 문서를 반환합니다 */
export function applyName(document) {
  if (!document) return document
  const name = overrides[document.id]
  return name ? { ...document, name } : document
}

/** 목록 전체에 적용 */
export function applyNames(documents = []) {
  return documents.map(applyName)
}

export function setName(id, name) {
  const next = String(name ?? '').trim()
  if (!id || !next) return
  overrides[id] = next
  persist()
}

/** 문서 삭제 시 함께 정리 */
export function clearName(id) {
  if (!(id in overrides)) return
  delete overrides[id]
  persist()
}

export function resetNames() {
  overrides = {}
  persist()
}
