/**
 * LLM 접속 설정 — 이 파일이 "모델 교체 지점"입니다.
 *
 * 코드를 고치지 않고 .env.local 값만 바꾸면 공급자 · 모델이 바뀝니다.
 *   상용 API  → BASE_URL/API_KEY/MODEL 세 줄
 *   sLLM      → BASE_URL 을 vLLM · Ollama · LM Studio 주소로 (MODEL 은 서빙 중인 이름)
 *   BE 프록시 → BASE_URL=/api/llm, API_KEY 는 비움 (키가 브라우저에서 사라집니다)
 *
 * OpenAI 호환 규격(POST {base}/chat/completions)을 기본으로 씁니다.
 * 대부분의 상용 API와 sLLM 서빙 스택이 이 규격을 그대로 지원하므로,
 * 규격이 다른 Anthropic 계열만 dialect 로 분기합니다.
 */
const env = import.meta.env

const bool = (value, fallback = false) => {
  if (value === undefined || value === '') return fallback
  return String(value) === 'true'
}
const num = (value, fallback) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export const llmConfig = {
  /** false 면 실제 호출 없이 목업 응답을 씁니다. */
  enabled: bool(env.VITE_LLM_ENABLED, false),
  /** 'openai' : OpenAI 호환 전부 · 'anthropic' : Claude Messages API */
  dialect: (env.VITE_LLM_DIALECT || 'openai').toLowerCase(),
  baseUrl: (env.VITE_LLM_BASE_URL || '').replace(/\/+$/, ''),
  apiKey: env.VITE_LLM_API_KEY || '',
  model: env.VITE_LLM_MODEL || '',
  temperature: num(env.VITE_LLM_TEMPERATURE, 0.2),
  maxTokens: num(env.VITE_LLM_MAX_TOKENS, 1024),
  /** 대화에 실어 보낼 최근 메시지 수 (토큰 · 비용 상한) */
  historyTurns: num(env.VITE_LLM_HISTORY_TURNS, 12),
}

/**
 * 호출 가능한 상태인지 확인합니다.
 * baseUrl 이 상대경로(/api/llm)면 키 없이도 정상입니다. 키는 백엔드가 붙입니다.
 */
export function llmReadiness() {
  if (!llmConfig.enabled) return { ok: false, reason: 'VITE_LLM_ENABLED 가 false 입니다' }
  if (!llmConfig.baseUrl) return { ok: false, reason: 'VITE_LLM_BASE_URL 이 비어 있습니다' }
  if (!llmConfig.model) return { ok: false, reason: 'VITE_LLM_MODEL 이 비어 있습니다' }

  const viaProxy = llmConfig.baseUrl.startsWith('/')
  if (!viaProxy && !llmConfig.apiKey) {
    return { ok: false, reason: 'VITE_LLM_API_KEY 가 비어 있습니다' }
  }
  return { ok: true, reason: '' }
}

export const LLM_READY = llmReadiness().ok
