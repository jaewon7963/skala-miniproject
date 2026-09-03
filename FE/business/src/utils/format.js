/** 화면 표기용 포맷 유틸 */

export function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '-'
  const mb = bytes / 1024 / 1024
  if (mb >= 1) return `${mb.toFixed(1)}MB`
  return `${Math.max(1, Math.round(bytes / 1024))}KB`
}

export function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatPercent(ratio) {
  if (ratio === null || ratio === undefined) return '-'
  return `${Math.round(ratio * 100)}%`
}
