/** Mock ↔ 실제 API 전환 스위치. 기본은 실제 API(`npm run dev`), 목업은 `npm run dev:mock`. */
export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK ?? 'false') === 'true'
