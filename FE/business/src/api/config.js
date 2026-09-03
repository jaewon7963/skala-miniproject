/** Mock ↔ 실제 API 전환 스위치 (.env 의 VITE_USE_MOCK) */
export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
